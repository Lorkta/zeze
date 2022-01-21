﻿using System;
using System.Xml;

namespace Zeze.Net
{
    public class Acceptor
    {
        public Service Service { get; private set; }
        public int Port { get; } = 0;
        public string Ip { get; } = string.Empty;
        public AsyncSocket Socket { get; private set; }
        public string Name => $"{Ip}:{Port}";

        public Acceptor(int port, string ip)
        {
            Port = port;
            Ip = ip;
        }

        public Acceptor(XmlElement self)
        {
            string attr = self.GetAttribute("Port");
            if (attr.Length > 0)
                Port = int.Parse(attr);
            Ip = self.GetAttribute("Ip");
        }

        internal void SetService(Service service)
        {
            lock (this)
            {
                if (Service != null)
                    throw new Exception($"Acceptor of '{Name}' Service != null");
                Service = service;
            }
        }

        public void Start()
        {
            lock (this)
            {
                if (null != Socket)
                    return;

                Socket = Ip.Length > 0
                    ? Service.NewServerSocket(Ip, Port, this)
                    : Service.NewServerSocket(System.Net.IPAddress.Any, Port, this);
            }
        }

        public void Stop()
        {
            lock (this)
            {
                Socket?.Dispose();
                Socket = null;
            }
        }
    }
}
