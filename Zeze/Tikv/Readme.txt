
Install GoLang
Install mingw-w64 (windows)
git clone https://github.com/tikv/client-go

copy tikv.go client-go/
copy build.bat client-go/
copy tikvbridge.h client-go/
copy tikvbridge.c client-go/

Edit client-go\txnkv\kv\memdb_buffer.go
�޸�����Ĵ��룬�ж�������nil��������Ϊ0�����顣
(
// Set associates key with value.
func (m *memDbBuffer) Set(k key.Key, v []byte) error {
	if len(v) == 0 {
		return errors.WithStack(ErrCannotSetNilValue)
	}

)
-->
(
// Set associates key with value.
func (m *memDbBuffer) Set(k key.Key, v []byte) error {
	if v == nil {
		return errors.WithStack(ErrCannotSetNilValue)
	}
)

run build.bat
