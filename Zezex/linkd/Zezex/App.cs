﻿
using System;
using System.Net.NetworkInformation;
using System.Text;
using System.Text.Json;
using Zeze.Util;
using Zeze.Net;

namespace Zezex
{
    public sealed partial class App
    {
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

        public override Zeze.IModule ReplaceModuleInstance(Zeze.IModule module)
        {
            return module;
        }


        public Config Config { get; private set; }
        public Zeze.Services.ServiceManager.Agent ServiceManagerAgent { get; private set; }
        public const string ServerServiceNamePrefix = "Game.Server.Module#";
        public const string LinkdServiceName = "Game.Linkd";
        private PersistentAtomicLong AsyncSocketSessionIdGen;

        private void LoadConfig()
        {
            try
            {
                string json = Encoding.UTF8.GetString(System.IO.File.ReadAllBytes("linkd.json"));
                Config = JsonSerializer.Deserialize<Config>(json);
            }
            catch (Exception)
            {
                //MessageBox.Show(ex.ToString());
            }
            if (null == Config)
                Config = new Config();
        }

        public string ProviderServicePassiveIp { get; private set; }
        public int ProviderServicePasivePort { get; private set; }

        public void Start()
        {
            LoadConfig();
            CreateZeze();
            CreateService();
            CreateModules();
            StartModules(); // 启动模块，装载配置什么的。
            Zeze.StartAsync().Wait(); // 启动数据库

            var (ip, port) = ProviderService.GetOnePassiveAddress();
            ProviderServicePassiveIp = ip;
            ProviderServicePasivePort = port;

            var linkName = $"{ProviderServicePassiveIp}:{ProviderServicePasivePort}";
            AsyncSocketSessionIdGen = PersistentAtomicLong.GetOrAdd("Linkd." + linkName);
            AsyncSocket.SessionIdGenFunc = AsyncSocketSessionIdGen.Next;

            StartService(); // 启动网络

            ServiceManagerAgent = new Zeze.Services.ServiceManager.Agent(Zeze);
            _ = ServiceManagerAgent.RegisterService(LinkdServiceName,
                linkName,
                ProviderServicePassiveIp, ProviderServicePasivePort);
        }

        public void Stop()
        {
            StopService(); // 关闭网络
            Zeze.Stop(); // 关闭数据库
            StopModules(); // 关闭模块,，卸载配置什么的。
            DestroyModules();
            DestroyService();
            DestroyZeze();
        }
    }
}
