package com.raincat.dolby_beta.hook;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import com.raincat.dolby_beta.helper.ClassHelper;
import com.raincat.dolby_beta.helper.EAPIHelper;
import com.raincat.dolby_beta.helper.SettingHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;


/**
 * <pre>
 *     author : RainCat
 *     e-mail : nining377@gmail.com
 *     time   : 2021/04/16
 *     desc   : 网络访问hook
 *     version: 1.0
 * </pre>
 */

public class EAPIHook {
    public EAPIHook(final Context context) {
        XposedBridge.hookMethod(ClassHelper.HttpResponse.getResultMethod(context), new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                //代理未开启
                if (!SettingHelper.getInstance().isEnable(SettingHelper.proxy_master_key))
                    return;
                //返回参数不对
                if ((!(param.getResult() instanceof String) && !(param.getResult() instanceof JSONObject)))
                    return;
                //返回参数为空
                String original = param.getResult().toString();
                if (TextUtils.isEmpty(original)) {
                    return;
                }
                ClassHelper.HttpResponse httpResponse = new ClassHelper.HttpResponse(param.thisObject);
                Object eapi = httpResponse.getEapi(context);
                Uri uri = ClassHelper.HttpUrl.getUri(context, eapi);
                if (!uri.getPath().contains("/eapi/"))
                    return;
                String path = uri.getPath();

                if (path.contains("song/enhance/player/url")) {
                    original = EAPIHelper.modifyPlayer(original);
                } else if (path.contains("song/enhance/download/url")) {
                    JSONObject jsonObject = new JSONObject(original);
                    JSONObject object = jsonObject.getJSONObject("data");
                    JSONArray array = new JSONArray();
                    array.put(object);
                    jsonObject.put("data", array);
                    original = EAPIHelper.modifyPlayer(jsonObject.toString())
                            .replace("[", "").replace("]", "");
                }

                param.setResult(param.getResult() instanceof JSONObject ? new JSONObject(original) : original);
            }
        });
    }
}
