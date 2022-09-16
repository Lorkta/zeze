package Zeze.Util;

import Zeze.AppBase;
import Zeze.Application;
import Zeze.Arch.Gen.GenModule;
import Zeze.Game.Rank;

/**
 * 1. 所有需要Redirect支持的内建模块在这里生成代码。
 * 2. 生成的代码提交到代码库中。
 * 3. 该模块创建的时候提供方法，直接创建生成的重载类。不需要用户生成代码。
 * 4. 这个类创建原始内建模块实例，并执行ReplaceModuleInstance生成代码。
 */
public final class RedirectGenMain {
	private RedirectGenMain() {
	}

	@SuppressWarnings("deprecation")
	public static void main(String[] args) {
		if (GenModule.instance.genFileSrcRoot == null) {
			System.out.println("usage: java -DGenFileSrcRoot=... -cp ... " + RedirectGenMain.class.getName());
			return;
		}

		var app = new AppBase() {
			@Override
			public Application getZeze() {
				return null;
			}
		};

		GenModule.instance.replaceModuleInstance(app, new Zeze.Arch.Online());
		GenModule.instance.replaceModuleInstance(app, new Zeze.Game.Online());
		GenModule.instance.replaceModuleInstance(app, new Rank());

		System.out.println("==================");
		System.out.println("Gen Redirect Done!");
	}
}
