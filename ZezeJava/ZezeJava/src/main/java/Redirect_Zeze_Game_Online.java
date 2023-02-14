// auto-generated @formatter:off
public final class Redirect_Zeze_Game_Online extends Zeze.Game.Online {
    private final Zeze.Arch.RedirectBase _redirect_;

    @Override
    protected void redirectRemoveLocal(int serverId, long roleId) {
        var _t_ = _redirect_.choiceServer(this, serverId);
        if (_t_ == null) { // local: loop-back
            _redirect_.runVoid(Zeze.Transaction.TransactionLevel.None,
                () -> super.redirectRemoveLocal(serverId, roleId));
            return;
        }

        var _p_ = new Zeze.Builtin.ProviderDirect.ModuleRedirect();
        var _a_ = _p_.Argument;
        _a_.setModuleId(11013);
        _a_.setRedirectType(Zeze.Builtin.ProviderDirect.ModuleRedirect.RedirectTypeToServer);
        _a_.setHashCode(serverId);
        _a_.setMethodFullName("Zeze.Game.Online:redirectRemoveLocal");
        _a_.setServiceNamePrefix(_redirect_.providerApp.serverServiceNamePrefix);
        var _b_ = Zeze.Serialize.ByteBuffer.Allocate();
        _b_.WriteLong(roleId);
        _a_.setParams(new Zeze.Net.Binary(_b_));

        _p_.Send(_t_, null);
    }

    @SuppressWarnings({"unchecked", "RedundantSuppression"})
    public Redirect_Zeze_Game_Online(Zeze.AppBase _app_) {
        super(_app_);
        _redirect_ = _app_.getZeze().redirect;

        _app_.getZeze().redirect.handles.put("Zeze.Game.Online:redirectRemoveLocal", new Zeze.Arch.RedirectHandle(
            Zeze.Transaction.TransactionLevel.None, (_hash_, _params_) -> {
                long roleId;
                var _b_ = _params_.Wrap();
                roleId = _b_.ReadLong();
                super.redirectRemoveLocal(_hash_, roleId);
                return null;
            }, null));
    }
}
