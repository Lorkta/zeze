package Zege;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import Zeze.Arch.Gen.GenModule;
import Zeze.Arch.LoadConfig;
import Zeze.Arch.Online;
import Zeze.Arch.ProviderApp;
import Zeze.Arch.ProviderDirect;
import Zeze.Arch.ProviderModuleBinds;
import Zeze.Arch.ProviderWithOnline;
import Zeze.Collections.DepartmentTree;
import Zeze.Collections.LinkedMap;
import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Netty.HttpServer;
import Zeze.Netty.Netty;
import Zeze.Util.Cert;
import Zeze.Util.JsonReader;
import Zeze.Util.PersistentAtomicLong;

public class App extends Zeze.AppBase {
    public static final App Instance = new App();
    public static App getInstance() {
        return Instance;
    }

    public ProviderApp ProviderApp;
    public ProviderDirect ProviderDirect;
    public ProviderWithOnline Provider;
    public LinkedMap.Module LinkedMaps;
    public DepartmentTree.Module DepartmentTrees;
    public Zeze.Netty.HttpServer HttpServer;

    private LoadConfig LoadConfig() {
        try {
            byte[] bytes = Files.readAllBytes(Paths.get("linkd.json"));
            return new JsonReader().buf(bytes).parse(LoadConfig.class);
            // return new ObjectMapper().readValue(bytes, LoadConfig.class);
        } catch (Exception e) {
            // e.printStackTrace();
        }
        return new LoadConfig();
    }

    public KeyStore FakeCa;
    private void createFakeCa() throws IOException, GeneralSecurityException {
        var file = "ZegeFakeCa.pkcs12";
        var passwd = "123";
        if (Files.exists(Path.of(file))) {
            FakeCa = Cert.loadKeyStore(new FileInputStream(file), passwd);
        } else {
            var rsa = Cert.generateRsaKeyPair();
            var cert = Cert.generate("ZegeFakeCa", rsa.getPublic(), "ZegeFakeCa", rsa.getPrivate(), 100000);
            FakeCa = KeyStore.getInstance("pkcs12");
            FakeCa.load(null, null);
            FakeCa.setKeyEntry("ZegeFakeCa", rsa.getPrivate(), passwd.toCharArray(), new Certificate[]{ cert });
            FakeCa.store(new FileOutputStream(file), passwd.toCharArray());
        }
    }

    public ZegeConfig ZegeConfig = new ZegeConfig();

    public void Start(String conf) throws Throwable {
        var config = new Config().addCustomize(ZegeConfig);
        config.loadAndParse(conf);

        createZeze(config);
        createService();

        HttpServer = new HttpServer(Zeze, null, 600);

        Provider = new ProviderWithOnline();
        ProviderDirect = new ProviderDirect();
        ProviderApp = new ProviderApp(Zeze, Provider, Server,
                "Zege.Server.Module#",
                ProviderDirect, ServerDirect, "Zege.Linkd", LoadConfig());
        Provider.Online = GenModule.instance.replaceModuleInstance(this, new Online(this));
        LinkedMaps = new LinkedMap.Module(Zeze);
        DepartmentTrees = new DepartmentTree.Module(Zeze, LinkedMaps);

        createModules();
        Zeze.start(); // 启动数据库
        startModules(); // 启动模块，装载配置什么的。
        Provider.Online.Start();
        HttpServer.start(new Netty(1), 80); //TODO: 从配置里读线程数和端口

        createFakeCa();

        PersistentAtomicLong socketSessionIdGen = PersistentAtomicLong.getOrAdd("Zege.Server." + Zeze.getConfig().getServerId());
        AsyncSocket.setSessionIdGenFunc(socketSessionIdGen::next);
        startService(); // 启动网络
        ProviderApp.StartLast(ProviderModuleBinds.Load(), modules);
    }

    public void Stop() throws Throwable {
        if (Provider != null && Provider.Online != null)
            Provider.Online.Stop();
        stopService(); // 关闭网络
        stopModules(); // 关闭模块，卸载配置什么的。
        if (Zeze != null)
            Zeze.stop(); // 关闭数据库
        destroyModules();
        destroyServices();
        destroyZeze();
    }

    // ZEZE_FILE_CHUNK {{{ GEN APP @formatter:off
    public Zeze.Application Zeze;
    public final java.util.HashMap<String, Zeze.IModule> modules = new java.util.HashMap<>();

    public Zege.Server Server;
    public Zege.ServerDirect ServerDirect;

    public Zege.User.ModuleUser Zege_User;
    public Zege.Friend.ModuleFriend Zege_Friend;
    public Zege.Message.ModuleMessage Zege_Message;

    @Override
    public Zeze.Application getZeze() {
        return Zeze;
    }

    public void createZeze() throws Throwable {
        createZeze(null);
    }

    public synchronized void createZeze(Zeze.Config config) throws Throwable {
        if (Zeze != null)
            throw new RuntimeException("Zeze Has Created!");

        Zeze = new Zeze.Application("Zege", config);
    }

    public synchronized void createService() throws Throwable {
        Server = new Zege.Server(Zeze);
        ServerDirect = new Zege.ServerDirect(Zeze);
    }

    public synchronized void createModules() {
        Zege_User = replaceModuleInstance(new Zege.User.ModuleUser(this));
        Zege_User.Initialize(this);
        if (modules.put(Zege_User.getFullName(), Zege_User) != null)
            throw new RuntimeException("duplicate module name: Zege_User");

        Zege_Friend = replaceModuleInstance(new Zege.Friend.ModuleFriend(this));
        Zege_Friend.Initialize(this);
        if (modules.put(Zege_Friend.getFullName(), Zege_Friend) != null)
            throw new RuntimeException("duplicate module name: Zege_Friend");

        Zege_Message = replaceModuleInstance(new Zege.Message.ModuleMessage(this));
        Zege_Message.Initialize(this);
        if (modules.put(Zege_Message.getFullName(), Zege_Message) != null)
            throw new RuntimeException("duplicate module name: Zege_Message");

        Zeze.setSchemas(new Zege.Schemas());
    }

    public synchronized void destroyModules() {
        Zege_Message = null;
        Zege_Friend = null;
        Zege_User = null;
        modules.clear();
    }

    public synchronized void destroyServices() {
        Server = null;
        ServerDirect = null;
    }

    public synchronized void destroyZeze() {
        Zeze = null;
    }

    public synchronized void startModules() throws Throwable {
        Zege_User.Start(this);
        Zege_Friend.Start(this);
        Zege_Message.Start(this);
    }

    public synchronized void stopModules() throws Throwable {
        if (Zege_Message != null)
            Zege_Message.Stop(this);
        if (Zege_Friend != null)
            Zege_Friend.Stop(this);
        if (Zege_User != null)
            Zege_User.Stop(this);
    }

    public synchronized void startService() throws Throwable {
        Server.Start();
        ServerDirect.Start();
    }

    public synchronized void stopService() throws Throwable {
        if (Server != null)
            Server.Stop();
        if (ServerDirect != null)
            ServerDirect.Stop();
    }
    // ZEZE_FILE_CHUNK }}} GEN APP @formatter:on
}
