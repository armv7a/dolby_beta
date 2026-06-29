package com.raincat.dolby_beta.helper;

import com.google.gson.Gson;
import com.raincat.dolby_beta.model.NeteaseSongListBean;

import java.util.ArrayList;

/**
 * <pre>
 *     author : RainCat
 *     e-mail : nining377@gmail.com
 *     time   : 2021/04/16
 *     desc   : 接口处理
 *     version: 1.0
 * </pre>
 */

public class EAPIHelper {
    private static final Gson gson = new Gson();

    /**
     * 解除下载加密
     */
    public static String modifyPlayer(String original) {
        NeteaseSongListBean listBean = gson.fromJson(original, NeteaseSongListBean.class);

        NeteaseSongListBean modifyListBean = new NeteaseSongListBean();
        modifyListBean.setCode(200);
        modifyListBean.setData(new ArrayList<>());
        for (NeteaseSongListBean.DataBean dataBean : listBean.getData()) {
            //flag与8非0为云盘歌曲
            if ((dataBean.getFlag() & 0x8) == 0) {

                dataBean.setFee(0);
                dataBean.setFlag(0);
                dataBean.setPayed(0);
                dataBean.setFreeTrialInfo(null);
                if (dataBean.getUrl() != null && dataBean.getUrl().contains("?"))
                    dataBean.setUrl(dataBean.getUrl().substring(0, dataBean.getUrl().indexOf("?")));
            }
            modifyListBean.getData().add(dataBean);
        }
        return gson.toJson(modifyListBean);
    }
}
