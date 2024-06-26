﻿using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Util;

namespace Zege
{
    public class Mission
    {
        public static void Run(Func<Task> func)
        {
            _ = CallAsync(func);
        }

        private static void OnUnhandledException(Exception ex)
        {
            var shell = Microsoft.Maui.Controls.Application.Current.MainPage;
            if (shell is AppShell appShell)
                MainThread.BeginInvokeOnMainThread(async () => await appShell.OnUnhandledException(ex));
            else if (shell is LoginShell loginShell)
                MainThread.BeginInvokeOnMainThread(async () => await loginShell.OnUnhandledException(ex));
        }

        public static async Task CallAsync(Func<Task<long>> func, Action<long> onerror = null)
        {
            try
            {
                var rc = await func();
                if (rc != 0)
                    onerror?.Invoke(rc);
            }
            catch (Exception ex)
            {
                onerror?.Invoke(ResultCode.Exception);
                OnUnhandledException(ex);
            }
        }

        public static async Task CallAsync(Func<Task> func, Action<long> onerror = null)
        {
            try
            {
                await func();
            }
            catch (Exception ex)
            {
                onerror?.Invoke(ResultCode.Exception);
                OnUnhandledException(ex);
            }
        }

        /// <summary>
        /// 成功（结果码为零）返回false；
        /// 失败（但低32位是skips里面的，返回true；
        /// 否则抛出异常。
        /// </summary>
        /// <param name="fullCode"></param>
        /// <param name="skips"></param>
        /// <returns></returns>
        /// <exception cref="Exception"></exception>
        public static bool VerifySkipResultCode(long fullCode, params int [] skips)
        {
            if (0 == fullCode)
                return false;

            var code = Zeze.IModule.GetErrorCode(fullCode);
            foreach (var skip in skips)
            {
                if (code == skip)
                    return true;
            }
            throw new Exception($"Error! Module={Zeze.IModule.GetModuleId(fullCode)} Code={code}");
        }
    }
}
