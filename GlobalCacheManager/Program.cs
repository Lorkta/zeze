﻿using System;
using System.Threading;

namespace GlobalCacheManager
{
    public class Program
    {
        public static void Main(string[] args)
        {
            string ip = null;
            int port = 5555;
            for (int i = 0; i < args.Length; ++i)
            {
                switch (args[i])
                {
                    case "-ip": ip = args[++i]; break;
                    case "-port": port = int.Parse(args[++i]); break;
                }
            }
            System.Net.IPAddress address = ip != null ? System.Net.IPAddress.Parse(ip) : System.Net.IPAddress.Any;
            Zeze.Services.GlobalCacheManager.Instance.Start(address, port);
            while (true)
            {
                Thread.Sleep(10000);
            }
        }
    }
}
