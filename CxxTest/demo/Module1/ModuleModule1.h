#pragma once

// ZEZE_FILE_CHUNK {{{ IMPORT GEN
#include "Gen/demo/Module1/AbstractModule.hpp"
// ZEZE_FILE_CHUNK }}} IMPORT GEN

namespace demo {
namespace Module1 {

class ModuleModule1 : public AbstractModule {
public:
    void Start();
    void Stop();

    virtual int64_t ProcessProtocol3(Zeze::Net::Protocol* _p) override;

    virtual int64_t ProcessProtocol4(Zeze::Net::Protocol* _p) override;

    virtual int64_t ProcessRpc2Request(Zeze::Net::Protocol* _r) override;

    // ZEZE_FILE_CHUNK {{{ GEN MODULE
    ModuleModule1(demo::App* app);
    // ZEZE_FILE_CHUNK }}} GEN MODULE
};
}
}
