
package main

/*
#include <stdlib.h>
#include "tikvbridge.h"
*/
import "C"
import (
	"context"
	"sync"
	"strings"
	"fmt"
	"unsafe"
	"bytes"

	"github.com/tikv/client-go/config"
	"github.com/tikv/client-go/key"
	"github.com/tikv/client-go/txnkv"
)

var ClientMutex sync.Mutex
var ClientIdSeed int = 0
var ClientMap sync.Map

var TransactionMutex sync.Mutex
var TransactionIdSeed int = 0
var TransactionMap sync.Map

//export NewClient
func NewClient(pdAddrs string, outerr []byte) int {
	_pdAddrs := strings.Split(pdAddrs, ",")
	var client, err = txnkv.NewClient(context.TODO(), _pdAddrs, config.Default())
	if err != nil {
		return -copy(outerr, err.Error())
	}

	ClientMutex.Lock()
	defer ClientMutex.Unlock()
	for true {
		ClientIdSeed += 1
		if ClientIdSeed < 0 {
			ClientIdSeed = 0
		}
		var _, exist = ClientMap.Load(ClientIdSeed)
		if exist {
			continue
		}
		ClientMap.Store(ClientIdSeed, client)
		return ClientIdSeed
	}
	return -copy(outerr, "Impossible!")
}

func DeleteClientMap(clientId int) *txnkv.Client {
	ClientMutex.Lock()
	defer ClientMutex.Unlock()
	var client, exist = ClientMap.Load(clientId)
	if exist {
		ClientMap.Delete(clientId)
		return client.(*txnkv.Client)
	}
	return nil
}

//export CloseClient
func CloseClient(clientId int, outerr []byte) int {
	var client = DeleteClientMap(clientId)
	if client != nil {
		err := client.Close()
		if err != nil {
			return -copy(outerr, err.Error())
		}
		return 0
	}
	return -copy(outerr, "ClientId Not Exist!")
}

//export Begin
func Begin(clientId int, outerr []byte) int {
	var _client, exist = ClientMap.Load(clientId)
	if exist {
		var client = _client.(*txnkv.Client)
		var tx, err = client.Begin(context.TODO())
		if err != nil {
			return -copy(outerr, err.Error())
		}
		TransactionMutex.Lock()
		defer TransactionMutex.Unlock()
		for true {
			TransactionIdSeed += 1
			if TransactionIdSeed < 0 {
				TransactionIdSeed = 0
			}
			var _, exist = TransactionMap.Load(TransactionIdSeed)
			if exist {
				continue
			}
			TransactionMap.Store(TransactionIdSeed, tx)
			return TransactionIdSeed
		}
		return -copy(outerr, "Impossible!")
	}
	return -copy(outerr, "ClientId Not Exist!")
}

func DeleteTransactionMap(txnId int)*txnkv.Transaction {
	TransactionMutex.Lock()
	defer TransactionMutex.Unlock()
	var tx, exist = TransactionMap.Load(txnId)
	if exist {
		TransactionMap.Delete(txnId)
		return tx.(*txnkv.Transaction)
	}
	return nil
}

//export Commit
func Commit(txnId int, outerr []byte) int {
	var tx = DeleteTransactionMap(txnId)
	if tx != nil {
		var err = tx.Commit(context.Background())
		if err != nil {
			return -copy(outerr, err.Error())
		}
		return 0
	}
	return -copy(outerr, "TransactionId Not Exist!")
}

//export Rollback
func Rollback(txnId int, outerr []byte) int {
	var tx = DeleteTransactionMap(txnId)
	if tx != nil {
		var err = tx.Rollback()
		if err != nil {
			return -copy(outerr, err.Error())
		}
		return 0
	}
	return -copy(outerr, "TransactionId Not Exist!")
}

//export Put
func Put(txnId int, key []byte, value []byte, outerr []byte) int {
	var _tx, exist = TransactionMap.Load(txnId)
	if exist {
		var tx = _tx.(*txnkv.Transaction)
		var err = tx.Set(key, value)
		if err != nil {
			return -copy(outerr, err.Error())
		}
		return 0
	}
	return -copy(outerr, "TransactionId Not Exist!")
}

//export Get
func Get(txnId int, key []byte, outvalue []byte, outerr []byte) int {
	var _tx, exist = TransactionMap.Load(txnId)
	if exist {
		var tx = _tx.(*txnkv.Transaction)
		var v, err = tx.Get(context.TODO(), key)
		if err != nil {
			return -copy(outerr, err.Error())
		}
		if v == nil {
			// nil 通过错误返回，不要随便改这个错误描述。
			return -copy(outerr, "ZezeSpecialError: value is nil.")
		}
		var copylen = copy(outvalue, v)
		var reallen = len(v)
		if copylen < reallen {
			// outvalue 不够大时，外面需要判断这个错误描述决定是否重试，不要随便改这个错误描述。
			return -copy(outerr, fmt.Sprintf("ZezeSpecialError: outvalue buffer not enough. BufferNeed=%d", reallen))
		}
		return copylen
	}
	return -copy(outerr, "TransactionId Not Exist!")
}

//export Delete
func Delete(txnId int, key[] byte, outerr []byte) int {
	var _tx, exist = TransactionMap.Load(txnId)
	if exist {
		var tx = _tx.(*txnkv.Transaction)
		var err = tx.Delete(key)
		if err != nil {
			return -copy(outerr, err.Error())
		}
		return 0
	}
	return -copy(outerr, "TransactionId Not Exist!") 
}

//export Scan
func Scan(txnId int, keyprefix []byte, walker unsafe.Pointer, outerr []byte) int {
	var _tx, exist = TransactionMap.Load(txnId)
	if exist {
		var tx = _tx.(*txnkv.Transaction)
		var it, err = tx.Iter(context.TODO(), key.Key(keyprefix), nil)
		if err != nil {
			return -copy(outerr, err.Error())
		}
		defer it.Close()
		var count int = 0

		var zeroptr = C.malloc(1)
		defer C.free(zeroptr)
		for it.Valid() {
			var _key = it.Key()
			var _value = it.Value()
			if !bytes.HasPrefix(_key, keyprefix) {
				return count
			}
			count++
			var keylen = len(_key)
			var valuelen = len(_value)
			var keyptr = zeroptr
			if keylen > 0 {
				keyptr = unsafe.Pointer(&_key[0])
			}
			var valueptr = zeroptr
			if valuelen > 0 {
				valueptr = unsafe.Pointer(&_value[0])
			}
			fmt.Println("keylen valuelen ", keylen, valuelen)

			var rwalker = C.BridgeWalker(walker, keyptr, C.int(keylen), valueptr, C.int(valuelen))
			if rwalker < 0 {
				return count
			}
			it.Next(context.TODO())
		}
		return count
	}
	return -copy(outerr, "TransactionId Not Exist!")	
}

func CheckError(rc int, err []byte) {
	if rc < 0 {
		var str = string(err[:-rc])
		panic(str)
	}
}

func Test() {
	var outerr = []byte("1111111111111111111111111111111111111111111111111111111111111111111111111")
	var clientId = NewClient("172.21.15.68:2379", outerr)
	CheckError(clientId, outerr)
	var transId = Begin(clientId, outerr)
	CheckError(transId, outerr)
	var rc int
	rc = Put(transId, []byte("key"), []byte(""), outerr)
	CheckError(rc, outerr)
	rc = Commit(transId, outerr)
	CheckError(rc, outerr)
	rc = CloseClient(clientId, outerr)
	CheckError(rc, outerr)
}

func main() {
//	Test()
}
