package Zeze.Arch.Gen;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import Zeze.AppBase;
import Zeze.Arch.RedirectAll;
import Zeze.Arch.RedirectAllFuture;
import Zeze.Arch.RedirectFuture;
import Zeze.Arch.RedirectHash;
import Zeze.Arch.RedirectToServer;
import Zeze.IModule;
import Zeze.Serialize.Serializable;
import Zeze.Util.InMemoryJavaCompiler;
import Zeze.Util.StringBuilderCs;

/**
 * 把模块的方法调用发送到其他服务器实例上执行。
 * 被重定向的方法用注解标明(RedirectToServer,RedirectHash,RedirectAll)。
 * 被重定向的方法需要是virtual的(非private非final非static的)。
 * <p>
 * 实现方案：
 * Game.App创建Module的时候调用回调。
 * 在回调中判断是否存在需要拦截的方法。
 * 如果需要就动态生成子类实现代码并编译并返回新的实例。
 * 可以提供和原来模块一致的接口。
 */
public final class GenModule {
	private static final String REDIRECT_PREFIX = "Redirect_";
	public static final GenModule instance = new GenModule();

	/**
	 * 源代码跟目录。
	 * 指定的时候，生成到文件，总是覆盖。
	 * 没有指定的时候，先查看目标类是否存在，存在则直接class.forName装载，否则生成到内存并动态编译。
	 */
	public String genFileSrcRoot = System.getProperty("GenFileSrcRoot"); // 支持通过给JVM传递-DGenFileSrcRoot=xxx参数指定
	private final InMemoryJavaCompiler compiler = new InMemoryJavaCompiler();
	private final HashMap<String, Class<?>> genClassMap = new HashMap<>();

	private GenModule() {
		compiler.ignoreWarnings();
	}

	public static <T extends IModule> Constructor<T> getCtor(Class<?> cls, AppBase app) throws ReflectiveOperationException {
		var appClass = app.getClass();
		@SuppressWarnings("unchecked")
		var ctors = (Constructor<T>[])cls.getDeclaredConstructors();
		for (var ctor : ctors) {
			if ((ctor.getModifiers() & (Modifier.PUBLIC | Modifier.PROTECTED)) != 0 &&
					ctor.getParameterCount() == 1 && ctor.getParameters()[0].getType().isAssignableFrom(appClass))
				return ctor;
		}
		for (var ctor : ctors) {
			if ((ctor.getModifiers() & (Modifier.PUBLIC | Modifier.PROTECTED)) != 0 && ctor.getParameterCount() == 0)
				return ctor;
		}
		throw new NoSuchMethodException("No suitable constructor for redirect module: " + cls.getName());
	}

	public static <T extends IModule> T newModule(Class<?> cls, AppBase app) throws ReflectiveOperationException {
		@SuppressWarnings("unchecked")
		var ctor = (Constructor<T>)getCtor(cls, app);
		if (ctor.getParameterCount() != 1)
			throw new NoSuchMethodException("No suitable constructor for redirect module: " + cls.getName());
		return ctor.newInstance(app);
	}

	private static String getRedirectClassName(Class<? extends IModule> moduleClass) {
		String className = moduleClass.getName();
		return className.startsWith(REDIRECT_PREFIX) ? className : REDIRECT_PREFIX + className.replace('.', '_');
	}

	public static <T extends IModule> T createRedirectModule(Class<T> moduleClass, AppBase app) {
		try {
			return newModule(Class.forName(GenModule.getRedirectClassName(moduleClass)), app);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings("unchecked")
	public synchronized <T extends IModule> T replaceModuleInstance(AppBase userApp, T module) {
		var modules = new IModule[]{module};
		replaceModuleInstances(userApp, modules);
		return (T)modules[0];
	}

	public synchronized void replaceModuleInstances(AppBase userApp, IModule[] modules) {
		try {
			var classNames = new String[modules.length];
			var classNameAndCodes = new HashMap<String, String>(); // <className, code>
			for (int i = 0, n = modules.length; i < n; i++) {
				var module = modules[i];
				if (module.getClass().getName().startsWith(REDIRECT_PREFIX)) // 预防二次replace
					continue;

				var overrides = new ArrayList<MethodOverride>();
				for (var method : module.getClass().getDeclaredMethods()) {
					for (var anno : method.getAnnotations()) {
						var type = anno.annotationType();
						if (type == RedirectToServer.class || type == RedirectHash.class || type == RedirectAll.class) {
							overrides.add(new MethodOverride(method, anno));
							break;
						}
					}
				}
				if (overrides.isEmpty())
					continue; // 没有需要重定向的方法。
				overrides.sort(Comparator.comparing(o -> o.method.getName())); // 按方法名排序，避免每次生成结果发生变化。

				String genClassName = getRedirectClassName(module.getClass());
				if (genFileSrcRoot == null) { // 不需要生成到文件的时候，尝试装载已经存在的生成模块子类。
					var genClass = genClassMap.get(genClassName);
					if (genClass == null) {
						try {
							genClass = Class.forName(genClassName);
							genClassMap.put(genClassName, genClass);
						} catch (ClassNotFoundException ignored) {
						}
					}
					if (genClass != null) {
						module.UnRegister();
						modules[i] = newModule(genClass, userApp);
						continue;
					}
				}

				var code = genModuleCode(genClassName, module, overrides, userApp);

				if (genFileSrcRoot != null) {
					byte[] oldBytes = null;
					byte[] newBytes = code.getBytes(StandardCharsets.UTF_8);
					var file = new File(genFileSrcRoot, genClassName + ".java");
					if (file.exists()) {
						oldBytes = Files.readAllBytes(file.toPath());
						if (Arrays.equals(oldBytes, newBytes))
							System.out.println("  Existed File: " + file.getAbsolutePath());
						else {
							System.out.println("Overwrite File: " + file.getAbsolutePath());
							oldBytes = null;
						}
					} else
						System.out.println("      New File: " + file.getAbsolutePath());
					if (oldBytes == null) {
						try (var fos = new FileOutputStream(file)) {
							fos.write(newBytes);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					continue; // 生成代码文件不会再继续执行，所以这里无需编译。
				}
				classNames[i] = genClassName;
				classNameAndCodes.put(genClassName, code);
			}

			var classNameAndClasses = compiler.compileAll(classNameAndCodes);
			genClassMap.putAll(classNameAndClasses);
			for (int i = 0, n = classNames.length; i < n; i++) {
				var className = classNames[i];
				if (className != null) {
					modules[i].UnRegister();
					modules[i] = newModule(classNameAndClasses.get(className), userApp);
				}
			}
		} catch (RuntimeException | Error e) {
			throw e;
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	private static String genModuleCode(String genClassName, IModule module, List<MethodOverride> overrides, AppBase userApp) throws Throwable {
		var sb = new StringBuilderCs();
		sb.appendLine("// auto-generated @" + "formatter:off");
		sb.appendLine("public final class {} extends {} {", genClassName, module.getClass().getName());
		sb.appendLine("    private final Zeze.Arch.RedirectBase _redirect_;");
		sb.appendLine();

		var sbHandles = new StringBuilderCs();
		for (var m : overrides) {
			var parametersDefine = m.getDefineString();
			var methodNameHash = m.method.getName();
			String returnName;
			var type = m.method.getReturnType();
			if (type == void.class)
				returnName = "void";
			else if (type == RedirectFuture.class)
				returnName = "Zeze.Arch.RedirectFuture<" + m.resultTypeName + '>';
			else if (type == RedirectAllFuture.class)
				returnName = "Zeze.Arch.RedirectAllFuture<" + m.resultTypeName + '>';
			else {
				throw new UnsupportedOperationException("Redirect return type Must Be void or RedirectFuture or RedirectAllFuture: "
						+ module.getClass().getName() + '.' + m.method.getName());
			}
			String modifier;
			int flags = m.method.getModifiers();
			if ((flags & Modifier.PUBLIC) != 0)
				modifier = "public ";
			else if ((flags & Modifier.PROTECTED) != 0)
				modifier = "protected ";
			else {
				throw new UnsupportedOperationException("Redirect method Must Be public or protected: "
						+ module.getClass().getName() + '.' + m.method.getName());
			}

			sb.appendLine("    @Override");
			sb.appendLine("    {}{} {}({}) {", modifier, returnName, m.method.getName(), parametersDefine); // m.getThrows() // 继承方法允许不标throws

			choiceTargetRunLoopback(sb, m, returnName);

			if (m.annotation instanceof RedirectAll) {
				genRedirectAll(sb, sbHandles, module, m);
				continue;
			}

			sb.appendLine("        var _p_ = new Zeze.Builtin.ProviderDirect.ModuleRedirect();");
			sb.appendLine("        var _a_ = _p_.Argument;");
			sb.appendLine("        _a_.setModuleId({});", module.getId());
			sb.appendLine("        _a_.setRedirectType({});", m.getRedirectType());
			sb.appendLine("        _a_.setHashCode({});", m.hashOrServerIdParameter.getName());
			sb.appendLine("        _a_.setMethodFullName(\"{}:{}\");", module.getFullName(), m.method.getName());
			sb.appendLine("        _a_.setServiceNamePrefix(_redirect_.providerApp.serverServiceNamePrefix);");
			if (m.inputParameters.size() > 0) {
				sb.appendLine("        var _b_ = Zeze.Serialize.ByteBuffer.Allocate();");
				Gen.instance.genEncode(sb, "        ", "_b_", m.inputParameters);
				sb.appendLine("        _a_.setParams(new Zeze.Net.Binary(_b_));");
			}
			sb.appendLine();
			if (returnName.equals("void"))
				sb.appendLine("        _p_.Send(_t_, null);");
			else {
				sb.appendLine("        var _f_ = new Zeze.Arch.RedirectFuture<{}>();", m.resultTypeName);
				sb.appendLine("        if (!_p_.Send(_t_, _rpc_ -> {");
				if (m.resultType == Long.class)
					sb.appendLine("            _f_.setResult(_rpc_.isTimeout() ? Zeze.Transaction.Procedure.Timeout : _rpc_.getResultCode());");
				else {
					if (!m.returnTypeHasResultCode) {
						sb.appendLine("            if (_rpc_.isTimeout()) {");
						sb.appendLine("                _f_.setResult(null);");
						sb.appendLine("                return Zeze.Transaction.Procedure.Success;");
						sb.appendLine("            }");
					}
					if ("String".equals(m.resultTypeName))
						sb.appendLine("            var _r_ = Zeze.Util.Str.fromBinary(_rpc_.Result.getParams());");
					else if ("Zeze.Net.Binary".equals(m.resultTypeName))
						sb.appendLine("            var _r_ = _rpc_.Result.getParams();");
					else
						sb.appendLine("            var _r_ = new {}();", m.resultTypeName);
					if (Serializable.class.isAssignableFrom(m.resultClass)) {
						sb.appendLine("            var _param_ = _rpc_.Result.getParams();");
						sb.appendLine("            if (_param_.size() > 0)");
						sb.appendLine("                _r_.decode(_param_.Wrap());");
						if (m.returnTypeHasResultCode)
							sb.appendLine("            _r_.setResultCode(_rpc_.isTimeout() ? Zeze.Transaction.Procedure.Timeout : _rpc_.getResultCode());");
					} else {
						if (!m.resultFields.isEmpty()) {
							sb.appendLine("            var _param_ = _rpc_.Result.getParams();");
							sb.appendLine("            if (_param_.size() > 0) {");
							sb.appendLine("                var _bb_ = _param_.Wrap();");
							for (var field : m.resultFields)
								Gen.instance.genDecode(sb, "                ", "_bb_", field.getType(), field.getGenericType(), "_r_." + field.getName());
							sb.appendLine("            }");
						}
						if (m.returnTypeHasResultCode)
							sb.appendLine("            _r_.resultCode = _rpc_.isTimeout() ? Zeze.Transaction.Procedure.Timeout : _rpc_.getResultCode();");
					}
					sb.appendLine("            _f_.setResult(_r_);");
				}
				sb.appendLine("            return Zeze.Transaction.Procedure.Success;");
				if (m.annotation instanceof RedirectHash)
					sb.appendLine("        }, {})) {", ((RedirectHash)m.annotation).timeout());
				else
					sb.appendLine("        }, {})) {", ((RedirectToServer)m.annotation).timeout());
				if (m.resultType == Long.class)
					sb.appendLine("            _f_.setResult(Zeze.Transaction.Procedure.ErrorSendFail);");
				else if (m.returnTypeHasResultCode) {
					sb.appendLine("            var _r_ = new {}();", m.resultTypeName);
					if (Serializable.class.isAssignableFrom(m.resultClass))
						sb.appendLine("            _r_.setResultCode(Zeze.Transaction.Procedure.ErrorSendFail);");
					else
						sb.appendLine("            _r_.resultCode = Zeze.Transaction.Procedure.ErrorSendFail;");
					sb.appendLine("            _f_.setResult(_r_);");
				} else
					sb.appendLine("            _f_.setResult(null);");
				sb.appendLine("        }");
				sb.appendLine("        return _f_;");
			}
			sb.appendLine("    }");
			sb.appendLine();

			// Handles
			sbHandles.appendLine("        _app_.getZeze().redirect.handles.put(\"{}:{}\", new Zeze.Arch.RedirectHandle(", module.getFullName(), m.method.getName());
			sbHandles.appendLine("            Zeze.Transaction.TransactionLevel.{}, (_hash_, _params_) -> {", m.transactionLevel);
			boolean genLocal = false;
			for (int i = 0; i < m.inputParameters.size(); ++i) {
				var p = m.inputParameters.get(i);
				Gen.instance.genLocalVariable(sbHandles, "                ", p);
				genLocal = true;
			}
			if (genLocal)
				sbHandles.appendLine("                var _b_ = _params_.Wrap();");
			Gen.instance.genDecode(sbHandles, "                ", "_b_", m.inputParameters);
			var normalCall = m.getNormalCallString();
			var sep = normalCall.isEmpty() ? "" : ", ";
			if (returnName.equals("void")) {
				sbHandles.appendLine("                super.{}(_hash_{}{});", methodNameHash, sep, normalCall);
				sbHandles.appendLine("                return null;");
			} else {
				if (normalCall.isEmpty())
					sbHandles.appendLine("                //noinspection CodeBlock2Expr");
				sbHandles.appendLine("                return super.{}(_hash_{}{});", methodNameHash, sep, normalCall);
			}
			if (m.resultType != null && Serializable.class.isAssignableFrom(m.resultClass)) {
				sbHandles.appendLine("            }, _result_ -> {");
				sbHandles.appendLine("                var _r_ = ({})_result_;", m.resultTypeName);
				sbHandles.appendLine("                int _s_ = _r_.preAllocSize();");
				sbHandles.appendLine("                var _b_ = Zeze.Serialize.ByteBuffer.Allocate(Math.min(_s_, 65536));");
				sbHandles.appendLine("                _r_.encode(_b_);");
				sbHandles.appendLine("                int _t_ = _b_.WriteIndex;");
				sbHandles.appendLine("                if (_t_ > _s_)");
				sbHandles.appendLine("                    _r_.preAllocSize(_t_);");
				sbHandles.appendLine("                return new Zeze.Net.Binary(_b_);");
				sbHandles.appendLine("            }));");
			} else if (!m.resultFields.isEmpty()) {
				sbHandles.appendLine("            }, _result_ -> {");
				sbHandles.appendLine("                var _r_ = ({})_result_;", m.resultTypeName);
				sbHandles.appendLine("                var _b_ = Zeze.Serialize.ByteBuffer.Allocate();");
				for (var field : m.resultFields)
					Gen.instance.genEncode(sbHandles, "                ", "_b_", field.getType(), "_r_." + field.getName());
				sbHandles.appendLine("                return new Zeze.Net.Binary(_b_);");
				sbHandles.appendLine("            }));");
			} else
				sbHandles.appendLine("            }, null));");
		}

		sb.appendLine("    @SuppressWarnings({\"unchecked\", \"RedundantSuppression\"})");
		var ctor = getCtor(module.getClass(), userApp);
		if (ctor.getParameterCount() == 1) {
			sb.appendLine("    public {}({} _app_) {", genClassName, ctor.getParameters()[0].getType().getName().replace('$', '.'));
			sb.appendLine("        super(_app_);");
		} else
			sb.appendLine("    public {}(Zeze.AppBase _app_) {", genClassName);
		sb.appendLine("        _redirect_ = _app_.getZeze().redirect;");
		sb.appendLine();
		sb.append(sbHandles.toString());
		sb.appendLine("    }");
		sb.appendLine("}");
		return sb.toString();
	}

	// 根据转发类型选择目标服务器，如果目标服务器是自己，直接调用基类方法完成工作。
	private static void choiceTargetRunLoopback(StringBuilderCs sb, MethodOverride m, String returnName) {
		if (m.annotation instanceof RedirectHash)
			sb.appendLine("        var _t_ = _redirect_.choiceHash(this, {}, {});",
					m.hashOrServerIdParameter.getName(), m.getConcurrentLevelSource());
		else if (m.annotation instanceof RedirectToServer)
			sb.appendLine("        var _t_ = _redirect_.choiceServer(this, {});", m.hashOrServerIdParameter.getName());
		else if (m.annotation instanceof RedirectAll)
			return; // RedirectAll 不在这里选择目标服务器。后面发送的时候直接查找所有可用服务器并进行广播。

		sb.appendLine("        if (_t_ == null) { // local: loop-back");
		if (returnName.equals("void")) {
			sb.appendLine("            _redirect_.runVoid(Zeze.Transaction.TransactionLevel.{},", m.transactionLevel);
			sb.appendLine("                () -> super.{}({}));", m.method.getName(), m.getBaseCallString());
			sb.appendLine("            return;");
		} else {
			sb.appendLine("            return _redirect_.runFuture(Zeze.Transaction.TransactionLevel.{},", m.transactionLevel);
			sb.appendLine("                () -> super.{}({}));", m.method.getName(), m.getBaseCallString());
		}
		sb.appendLine("        }");
		sb.appendLine();
	}

	private static void genRedirectAll(StringBuilderCs sb, StringBuilderCs sbHandles,
									   IModule module, MethodOverride m) throws Throwable {
		sb.append("        var _c_ = new Zeze.Arch.RedirectAllContext<>({}, ", m.hashOrServerIdParameter.getName());
		if (m.resultTypeName != null) {
			sb.appendLine("_params_ -> {");
			sb.appendLine("            var _r_ = new {}();", m.resultTypeName);
			sb.appendLine("            if (_params_ != null) {");
			sb.appendLine("                var _b_ = _params_.Wrap();");
			for (var field : m.resultFields)
				Gen.instance.genDecode(sb, "                ", "_b_", field.getType(), field.getGenericType(), "_r_." + field.getName());
			sb.appendLine("            }");
			sb.appendLine("            return _r_;");
			sb.appendLine("        });");
		} else
			sb.appendLine("null);");
		sb.appendLine("        var _p_ = new Zeze.Builtin.ProviderDirect.ModuleRedirectAllRequest();");
		sb.appendLine("        var _a_ = _p_.Argument;");
		sb.appendLine("        _a_.setModuleId({});", module.getId());
		sb.appendLine("        _a_.setHashCodeConcurrentLevel({});", m.hashOrServerIdParameter.getName());
		sb.appendLine("        _a_.setMethodFullName(\"{}:{}\");", module.getFullName(), m.method.getName());
		sb.appendLine("        _a_.setServiceNamePrefix(_redirect_.providerApp.serverServiceNamePrefix);");
		sb.appendLine("        _a_.setSessionId(_redirect_.providerApp.providerDirectService.addManualContextWithTimeout(_c_, {}));", ((RedirectAll)m.annotation).timeout());
		if (m.inputParameters.size() > 0) {
			sb.appendLine("        var _b_ = Zeze.Serialize.ByteBuffer.Allocate();");
			Gen.instance.genEncode(sb, "        ", "_b_", m.inputParameters);
			sb.appendLine("        _a_.setParams(new Zeze.Net.Binary(_b_));");
		}
		if (m.resultType != null)
			sb.appendLine("        return _redirect_.redirectAll(this, _p_, _c_);");
		else
			sb.appendLine("        _redirect_.redirectAll(this, _p_, _c_);");
		sb.appendLine("    }");
		sb.appendLine();

		// handles
		sbHandles.appendLine("        _app_.getZeze().redirect.handles.put(\"{}:{}\", new Zeze.Arch.RedirectHandle(", module.getFullName(), m.method.getName());
		sbHandles.appendLine("            Zeze.Transaction.TransactionLevel.{}, (_hash_, _params_) -> {", m.transactionLevel);
		if (!m.inputParameters.isEmpty()) {
			sbHandles.appendLine("                var _b_ = _params_.Wrap();");
			for (int i = 0; i < m.inputParameters.size(); ++i) {
				var p = m.inputParameters.get(i);
				Gen.instance.genLocalVariable(sbHandles, "                ", p);
			}
			Gen.instance.genDecode(sbHandles, "                ", "_b_", m.inputParameters);
		}

		var normalCall = m.getNormalCallString();
		if (m.resultType != null) {
			if (normalCall.isEmpty())
				sbHandles.appendLine("                //noinspection CodeBlock2Expr");
			sbHandles.appendLine("                return super.{}(_hash_{}{});", m.method.getName(), normalCall.isEmpty() ? "" : ", ", normalCall);
		} else {
			sbHandles.appendLine("                super.{}(_hash_{}{});", m.method.getName(), normalCall.isEmpty() ? "" : ", ", normalCall);
			sbHandles.appendLine("                return null;");
		}
		if (m.resultTypeName != null && !m.resultFields.isEmpty()) {
			sbHandles.appendLine("            }, _result_ -> {");
			sbHandles.appendLine("                var _r_ = ({})_result_;", m.resultTypeName);
			sbHandles.appendLine("                var _b_ = Zeze.Serialize.ByteBuffer.Allocate();");
			for (var field : m.resultFields)
				Gen.instance.genEncode(sbHandles, "                ", "_b_", field.getType(), "_r_." + field.getName());
			sbHandles.appendLine("                return new Zeze.Net.Binary(_b_);");
			sbHandles.appendLine("            }));");
		} else
			sbHandles.appendLine("            }, null));");
	}
}
