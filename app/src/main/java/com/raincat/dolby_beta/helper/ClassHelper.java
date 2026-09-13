package com.raincat.dolby_beta.helper;

import android.content.Context;
import android.net.Uri;

import com.annimon.stream.Stream;

import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.dexbacked.DexBackedClassDef;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.iface.MultiDexContainer;

import java.io.Closeable;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import de.robv.android.xposed.XposedHelpers;

import static de.robv.android.xposed.XposedHelpers.findClassIfExists;

/**
 * <pre>
 *     author : RainCat
 *     e-mail : nining377@gmail.com
 *     time   : 2021/04/14
 *     desc   : 类加载帮助
 *     version: 1.0
 * </pre>
 */

public class ClassHelper {
    //类加载器
    private static ClassLoader classLoader = null;
    //dex缓存
    private static List<String> classCacheList = null;
    //dex缓存路径
    private static String classCachePath = null;
    //网易云版本
    private static int versionCode = 0;

    public static synchronized void getCacheClassList(final Context context, final int version, final OnCacheClassListener listener) {
        if (classLoader == null) {
            classLoader = context.getClassLoader();
            versionCode = version;
            File cacheFile = Objects.requireNonNull(context.getExternalFilesDir(null));
            if (cacheFile.exists() || cacheFile.mkdirs())
                classCachePath = cacheFile.getPath();
        }
        if (classCacheList == null) {
            classCacheList = new ArrayList<>();
            if (classCacheList.size() == 0) {
                new Thread(() -> getCacheClassByZip(context, version, listener)).start();
            } else
                listener.onGet();
        } else
            listener.onGet();
    }

    private static synchronized void getCacheClassByZip(Context context, int version, OnCacheClassListener listener) {
        try {
            // 不用 ZipDexContainer 因为会验证zip里面的文件是不是dex，会慢一点
            File appInstallFile = new File(context.getPackageResourcePath());
            Enumeration<? extends ZipEntry> zip = new ZipFile(appInstallFile).entries();
            while (zip.hasMoreElements()) {
                ZipEntry dexInZip = zip.nextElement();
                if (dexInZip.getName().startsWith("classes") && dexInZip.getName().endsWith(".dex")) {
                    MultiDexContainer.DexEntry<? extends DexBackedDexFile> dexEntry = DexFileFactory.loadDexEntry(appInstallFile, dexInZip.getName(), true, null);
                    DexBackedDexFile dexFile = dexEntry.getDexFile();
                    for (DexBackedClassDef classDef : dexFile.getClasses()) {
                        String classType = classDef.getType();
                        if (classType.contains("com/netease/cloudmusic") || classType.contains("okhttp3")) {
                            classType = classType.substring(1, classType.length() - 1).replace("/", ".");
                            classCacheList.add(classType);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            FileHelper.writeFileFromSD(classCachePath + File.separator + "class-" + version, classCacheList);
            listener.onGet();
        }
    }

    public interface OnCacheClassListener {
        void onGet();
    }

    public static List<String> getFilteredClasses(Pattern pattern, Comparator<String> comparator) {
        List<String> list = Stream.of(classCacheList)
                .filter(s -> pattern.matcher(s).find())
                .toList();
        Collections.sort(list, comparator);
        return list;
    }

    private static Class<?> getClassByXposed(String className) {
        Class<?> clazz = findClassIfExists(className, classLoader);
        if (clazz == null)
            clazz = findClassIfExists("com.netease.cloudmusic.NeteaseMusicApplication", classLoader);
        return clazz;
    }

    public static class OKHttp3Response {
        private static Class<?> clazz;

        final Object okHttp3Response;

        public OKHttp3Response(Object okHttp3Response) {
            this.okHttp3Response = okHttp3Response;
        }

        static Class<?> getClazz(Context context) {
            if (clazz == null) {
                Pattern pattern = Pattern.compile("^okhttp3\\.[a-zA-Z]{1,8}$");
                List<String> list = ClassHelper.getFilteredClasses(pattern, Collections.reverseOrder());

                clazz = Stream.of(list)
                        .map(ClassHelper::getClassByXposed)
                        .filter(c -> !Modifier.isAbstract(c.getModifiers()))
                        .filter(c -> Modifier.isPublic(c.getModifiers()))
                        .filter(c -> Modifier.isFinal(c.getModifiers()))
                        .filter(c -> c.getInterfaces().length == 1)
                        .filter(c -> c.getInterfaces()[0] == Closeable.class)
                        .filter(c -> Stream.of(c.getDeclaredFields()).anyMatch(m -> m.getType() == int.class))
                        .filter(c -> Stream.of(c.getDeclaredFields()).anyMatch(m -> m.getType() == String.class))
                        .filter(c -> Stream.of(c.getDeclaredFields()).anyMatch(m -> m.getType() == long.class))
                        .findFirst()
                        .get();
            }
            return clazz;
        }

        public Object getHeadersObject(Context context) throws IllegalAccessException, NullPointerException {
            Field[] fields = getClazz(context).getDeclaredFields();
            Field dataField = Stream.of(fields)
                    .filter(f -> Stream.of(f.getType()).anyMatch(pf -> pf == OKHttp3Header.getClazz(context)))
                    .filter(f -> Stream.of(f.getType().getDeclaredFields()).anyMatch(pf -> pf.getType() == String[].class))
                    .findFirst().get();

            dataField.setAccessible(true);
            return dataField.get(okHttp3Response);
        }
    }

    public static class OKHttp3Header {
        private static Class<?> clazz;

        final Object okHttp3Header;

        public OKHttp3Header(Object okHttp3Header) {
            this.okHttp3Header = okHttp3Header;
        }

        static Class<?> getClazz(Context context) {
            if (clazz == null) {
                Pattern pattern = Pattern.compile("^okhttp3\\.[a-zA-Z]{1,7}$");
                List<String> list = ClassHelper.getFilteredClasses(pattern, Collections.reverseOrder());

                clazz = Stream.of(list)
                        .map(ClassHelper::getClassByXposed)
                        .filter(c -> !Modifier.isAbstract(c.getModifiers()))
                        .filter(c -> Modifier.isPublic(c.getModifiers()))
                        .filter(c -> Modifier.isFinal(c.getModifiers()))
                        .filter(c -> Stream.of(c.getDeclaredFields()).anyMatch(m -> m.getType() == String[].class))
                        .findFirst()
                        .get();
            }
            return clazz;
        }

        public String[] getHeaders(Context context) throws IllegalAccessException, NullPointerException {
            Field[] fields = getClazz(context).getDeclaredFields();
            Field dataField = Stream.of(fields)
                    .filter(f -> Stream.of(f.getType()).anyMatch(pf -> pf == String[].class))
                    .findFirst().get();

            dataField.setAccessible(true);
            return (String[]) dataField.get(okHttp3Header);
        }
    }

    /**
     * 获取请求返回
     */
    public static class HttpResponse {
        private static Class<?> clazz;
        private static Method getResultMethod;

        final Object httpResponse;

        public HttpResponse(Object httpResponse) {
            this.httpResponse = httpResponse;
        }

        static Class<?> getClazz(Context context) {
            if (clazz == null) {
                Pattern pattern;
                if (versionCode < 154)
                    pattern = Pattern.compile("^com\\.netease\\.cloudmusic\\.[a-z]\\.[a-z]\\.[a-z]\\.[a-z]$");
                else
                    pattern = Pattern.compile("^com\\.netease\\.cloudmusic\\.network\\.[a-z]\\.[a-z]\\.[a-z]$");
                List<String> list = ClassHelper.getFilteredClasses(pattern, Collections.reverseOrder());

                clazz = Stream.of(list)
                        .map(ClassHelper::getClassByXposed)
                        .filter(c -> !Modifier.isAbstract(c.getModifiers()))
                        .filter(c -> Modifier.isPublic(c.getModifiers()))
                        .filter(c -> Modifier.isFinal(c.getModifiers()))
                        .filter(c -> c.getSuperclass() == Object.class)
                        .filter(c -> Stream.of(c.getDeclaredFields()).anyMatch(m -> m.getType() == OKHttp3Response.getClazz(context)))
                        .findFirst()
                        .get();
            }
            return clazz;
        }

        public Object getResponseObject(Context context) throws IllegalAccessException, NullPointerException {
            Field[] fields = getClazz(context).getDeclaredFields();
            Field dataField = Stream.of(fields)
                    .filter(f -> Stream.of(f.getType().getInterfaces()).anyMatch(i -> i == Closeable.class))
                    .filter(f -> Stream.of(f.getType().getDeclaredFields()).anyMatch(pf -> pf.getType().getName().startsWith("okhttp3")))
                    .findFirst().get();

            dataField.setAccessible(true);
            return dataField.get(httpResponse);
        }

        public Object getEapi(Context context) throws IllegalAccessException, NullPointerException {
            Field[] fields = getClazz(context).getDeclaredFields();
            Field dataField = Stream.of(fields)
                    .filter(c -> Modifier.isAbstract(c.getType().getModifiers()))
                    .filter(c -> c.getType().getSuperclass() == Object.class)
                    .filter(c -> Stream.of(c.getType().getDeclaredFields()).anyMatch(m -> m.getType().getName().startsWith("okhttp3")))
                    .findFirst().get();

            dataField.setAccessible(true);
            return dataField.get(httpResponse);
        }

        public static Method getResultMethod(Context context) {
            if (getResultMethod == null) {
                List<Method> methodList = Arrays.asList(getClazz(context).getDeclaredMethods());
                getResultMethod = Stream.of(methodList)
                        .filter(m -> m.getExceptionTypes().length == 2)
                        .findFirst()
                        .get();
            }
            return getResultMethod;
        }
    }

    /**
     * 获取请求URL
     */
    public static class HttpUrl {
        private static Class<?> clazz;

        static Class<?> getClazz(Context context) {
            if (clazz == null) {
                Pattern pattern;
                if (versionCode < 154)
                    pattern = Pattern.compile("^com\\.netease\\.cloudmusic\\.[a-z]\\.[a-z]\\.[a-z]\\.[a-z]$");
                else
                    pattern = Pattern.compile("^com\\.netease\\.cloudmusic\\.network\\.[a-z]\\.[a-z]\\.[a-z]$");
                List<String> list = ClassHelper.getFilteredClasses(pattern, Collections.reverseOrder());

                clazz = Stream.of(list)
                        .map(ClassHelper::getClassByXposed)
                        .filter(c -> Modifier.isAbstract(c.getModifiers()))
                        .filter(c -> Modifier.isPublic(c.getModifiers()))
                        .filter(c -> c.getSuperclass() == Object.class)
                        .filter(c -> Stream.of(c.getDeclaredFields()).anyMatch(m -> m.getType().getName().startsWith("okhttp3")))
                        .findFirst()
                        .get();
            }
            return clazz;
        }

        public static Uri getUri(Context context, Object eapi) throws IllegalAccessException, NullPointerException {
            Field uriField = XposedHelpers.findFirstFieldByExactType(getClazz(context), Uri.class);
            uriField.setAccessible(true);
            return (Uri) uriField.get(eapi);
        }
    }

    /**
     * 拦截器
     */
    public static class HttpInterceptor {
        private static Class<?> clazz;
        private static List<Method> methodList;

        static Class<?> getClazz(Context context) {
            if (clazz == null) {
                Pattern pattern;
                if (versionCode < 154)
                    pattern = Pattern.compile("^com\\.netease\\.cloudmusic\\.[a-z]\\.[a-z]\\.[a-z]");
                else
                    pattern = Pattern.compile("^com\\.netease\\.cloudmusic\\.network\\.[a-z]");
                List<String> list = ClassHelper.getFilteredClasses(pattern, Collections.reverseOrder());
                clazz = Stream.of(list)
                        .map(ClassHelper::getClassByXposed)
                        .filter(c -> c.getInterfaces().length == 1)
                        .filter(c -> Stream.of(c.getInterfaces()).anyMatch(i -> i.getName().contains("Interceptor")))
                        .filter(c -> !Modifier.isAbstract(c.getModifiers()))
                        .filter(c -> Modifier.isPublic(c.getModifiers()))
                        .filter(c -> Stream.of(c.getDeclaredMethods()).anyMatch(m -> m.getReturnType().getName().contains("Pair")))
                        .findFirst()
                        .get();
            }
            return clazz;
        }

        public static List<Method> getMethodList(Context context) {
            if (methodList == null) {
                methodList = new ArrayList<>();
                methodList.addAll(Stream.of(getClazz(context).getDeclaredMethods())
                        .filter(m -> m.getExceptionTypes().length == 1)
                        .filter(m -> m.getParameterTypes().length == 5)
                        .filter(m -> m.getReturnType().getName().contains("Response"))
                        .toList());
            }
            return methodList;
        }
    }
}
