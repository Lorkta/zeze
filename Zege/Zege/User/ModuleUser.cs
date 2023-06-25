
using System.Collections.Concurrent;
using System.Runtime.ConstrainedExecution;
using System.Security.Cryptography.X509Certificates;
using Zeze.Net;
using Zeze.Util;

namespace Zege.User
{
    public partial class ModuleUser : AbstractModule
    {
        public void Start(global::Zege.App app)
        {
        }

        public void Stop(global::Zege.App app)
        {
        }

        public string Account { get; private set; }
        public X509Certificate2 Certificate { get; private set; }
        private readonly ConcurrentDictionary<string, ConcurrentDictionary<long, X509Certificate2>> Certificates = new();
        private string savedPasswd;

        public int ServerId;
        public String NotifyId;

        public async Task<X509Certificate2> GetLastPrivateCertificate(string account)
        {
            var last = await SecureStorage.Default.GetAsync(account + ".LastCertIndex");
            if (null == last)
                return null;

            var index = long.Parse(last);
            return await GetPrivateCertificate(account, index);
        }

        public async Task<X509Certificate2> GetPrivateCertificate(string account, long index)
        {
            // cache
            if (Certificates.TryGetValue(account, out var certs) && certs.TryGetValue(index, out var cert))
                return cert;

            // storage
            var pkcs12 = await SecureStorage.Default.GetAsync(Account + "." + index + ".pkcs12");
            if (pkcs12 == null || pkcs12.Length == 0)
                return null;
            cert = Cert.CreateFromPkcs12(pkcs12, savedPasswd);
            Certificates[account][index] = cert;
            return cert;
        }

        public async Task<int> OpenCertAsync(string account, string passwd, bool save)
        {
            if (null == passwd)
                passwd = string.Empty;

            var certSaved = await SecureStorage.Default.GetAsync(account + ".pkcs12");
            if (null == certSaved)
                return 1;

            var pkcs12 = Convert.FromBase64String(certSaved);
            if (null == passwd)
                passwd = await SecureStorage.Default.GetAsync(account + ".password");
            savedPasswd = passwd;
            Certificate = Cert.CreateFromPkcs12(pkcs12, passwd);

            // 证书打开成功以后，才进行密码修改或者删除。
            if (save)
            {
                await SecureStorage.Default.SetAsync(account + ".password", passwd);
            }
            else
            {
                SecureStorage.Default.Remove(account + ".password");
            }
            Account = account;
            return 0;
        }

        public async Task<int> CreateAccountAsync(string account, string passwd, bool savedPasswd)
        {
            if (null == passwd)
                passwd = string.Empty;

            var certSaved = await SecureStorage.Default.GetAsync(account + ".pkcs12");
            if (certSaved != null)
                return eAccountHasUsed;

            var p = new Prepare();
            p.Argument.Account = account;
            await p.SendAsync(App.Connector.TryGetReadySocket());
            if (Mission.VerifySkipResultCode(p.ResultCode, eAccountHasUsed))
                return eAccountHasUsed;

            var c = new Create();
            var rsa = Cert.GenerateRsa();
            var sign = Cert.Sign(rsa, p.Result.RandomData.GetBytesUnsafe());
            c.Argument.Account = account;
            c.Argument.RsaPublicKey = new Binary(rsa.ExportRSAPublicKey());
            c.Argument.Signed = new Binary(sign);
            await c.SendAsync(App.Connector.TryGetReadySocket());
            Mission.VerifySkipResultCode(c.ResultCode);

            var cert = Cert.CreateFromCertAndPrivateKey(c.Result.Cert.GetBytesUnsafe(), rsa);
            if (savedPasswd)
                await SecureStorage.Default.SetAsync(account + ".password", passwd);
            var pkcs12 = cert.Export(X509ContentType.Pkcs12, passwd);
            var base64 = Convert.ToBase64String(pkcs12);
            await SecureStorage.Default.SetAsync(account + "." + c.Result.LastCertIndex + ".pkcs12", base64);
            await SecureStorage.Default.SetAsync(account + ".LastCertIndex", c.Result.LastCertIndex.ToString());
            return 0;
        }
    }
}
