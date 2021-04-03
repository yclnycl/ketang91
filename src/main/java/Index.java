import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.StaticLog;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.script.ScriptException;
import java.io.File;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.regex.Pattern;

public class Index {

    public static Set<String> list = new HashSet<>();

    public static Map<String, String> cookie = new HashMap<>(){{
        put("Cookie", "uniqueVisitorId=c6ecaabc-27de-ce32-4d88-702cbdcf0527; Hm_lvt_80dc68f625454799fff627010e77e7df=1615952235,1617361683,1617409083; XSRF-TOKEN=eyJpdiI6IjFGXC9cLzhwcTkxUmVaZ0pCaWFZbUhkZz09IiwidmFsdWUiOiJFQURDayszV3o0UStrM3FZY2lpNEt6dFNyUWlOSjF2VlV2cjRVRW1mNFwveFlQMkZ6d0E3TTh1YWtwS0hNcWVhSEFUSndMZHBRMGo2TnBhNHNYa1wvcWtRPT0iLCJtYWMiOiIwNzNhZDk2MjlkMDBhNTcwYjA1MWY3ZTYzNjkxOGVlOGQzN2JjZTg1MzBmYWQyMDZmM2NmM2U5YzMxYjIyMjI2In0%3D; laravel_session=eyJpdiI6IittOWNuSmRkcEMrREJKYzY2S0VGNmc9PSIsInZhbHVlIjoiS3loVjhuMnZ6R3lNUUNuOTN1M2ZOTjNnZTRFakd4UWhlNHFXdkljZ0hXZWJXZjl3QVwvbmswa0xlanBWT3NBaEFWK29PaDJ1ZkZSWVFIUHhSWmhZcGhRPT0iLCJtYWMiOiJmMjlhYTA5ZTU4YTFhNDE0MzQ1OGUwMGM2MGM5NGRiMGI5ZWRmZGYyZjk1NTliMjM2MmIwZjg2NTY3MWQ2MjkwIn0%3D; Hm_lpvt_80dc68f625454799fff627010e77e7df=1617409095");
    }};

    static String videoPath = "C:\\video";

    static ExecutorService executorService = ThreadUtil.newExecutor(9);

    public static void main(String[] args) throws Exception {

        var url = "http://ketang91.com/lessons";

        //访问用户中心
        var userInfoPage1 = getUserInfoPage(url);

        var parse = Jsoup.parse(userInfoPage1);

        //因为用户中心的全部课程不包括免费课程所以要分开爬取
        //这是是获取所有的左侧模块课程
        getAllItem(parse);

        for (int page = 1; page <= 7; page++) {
            //目前用户中心有7页 暂时写死7七页
            var userInfoPage = getUserInfoPage(page);
            var parse1 = Jsoup.parse(userInfoPage);
            //解析所有shop item
            getShopItem(parse1);
        }

        for (String s : list) {

            var userInfoPage = getUserInfoPage(s);

            //这种是不包含播放列表的
            if(s.contains("vip_single_channel")) {

                vip_single_channel_html(userInfoPage);

            }
            //这种是包含播放列表的
            else if(s.contains("vip_channel")) {

                vip_channel(userInfoPage);

            }
        }

        executorService.shutdown();

    }

    public static String getUserInfoPage(String page) {

        StaticLog.info("{}开始爬取", page);

        //这里取巧直接把登录生成的cookie放到http请求了没做登陆识别
        var cookie = HttpRequest.get(page).headerMap(Index.cookie, true).execute();

        return cookie.body();
    }

    public static String getUserInfoPage(int page) {
        String url = "http://ketang91.com/lessons?page="+page;

        StaticLog.info("{}开始爬取", url);

        var cookie = HttpRequest.get(url).headerMap(Index.cookie, true).execute();

        return cookie.body();
    }

    public static void getShopItem(Document parse) throws Exception {

        var select = parse.select(".shop-item");

        if(select.size() == 0) {
            throw new Exception("当前页面没有shopitem");
        }
        for (Element element : select) {
            var href = element.attr("href");

            Index.list.add(String.format("http://ketang91.com%s", href));
        }

    }

    public static void getAllItem(Document parse) {

        var select = parse.select("#left_self table a");
        for (Element element : select) {
            var href = element.attr("href");

            for (int page = 1; page <= 7; page++) {
                var url = "http://ketang91.com"+href+"?page="+page;
                var userInfoPage = getUserInfoPage(url);
                var parse1 = Jsoup.parse(userInfoPage);

                try{
                    getShopItem(parse1);
                }catch (Exception e) {
                    StaticLog.debug("当前页面没有shop item了");
                    break;
                }

            }
        }

    }

    public static void vip_single_channel_html(String boyd) throws ScriptException {


        var page = Jsoup.parse(boyd);

        //获取页面js
        var select = page.select("script");

        var text = select.get(select.size() - 1).html();

        //获取m3u8url
        var m3u8Url = evalJs(text);

        var title = page.select("title").text();

        downloadVideo(m3u8Url, title);


    }

    public static void vip_channel(String boyd) {

        var parse = Jsoup.parse(boyd);

        var select = parse.select(".list-group-item");

        for (Element element : select) {

            var onclick = element.attr("onclick");
            var text = element.text();

            //正则解析vid
            var compile = Pattern.compile("'.*?'");

            var matcher = compile.matcher(onclick);

            List<String> vidAndStid = new ArrayList<>();
            if(matcher.find()) {

                var group = matcher.group(0);

                vidAndStid.add(group.substring(1, group.length() - 1));
            }

            //发现stid是固定的就写死了
            vidAndStid.add("9DD53F70C341DB9B");

            var m3u8Url = getM3u8Url(vidAndStid);

            var title = parse.select("title").text();

            downloadVideo(m3u8Url, title, text);
        }


    }

    public static String evalJs(String js) throws ScriptException {

        //正则解析变量
        var compile = Pattern.compile("='.*?';");

        var matcher = compile.matcher(js);

        List<String> vidAndStid = new ArrayList<>();

        while(matcher.find()) {

            var group = matcher.group();
            vidAndStid.add(group.substring(2, group.length()-2));
        }

        return getM3u8Url(vidAndStid);

    }

    public static String getM3u8Url(List<String> list){
        String url = String.format("https://p.bokecc.com/servlet/getvideofile?vid=%s&siteid=%s", list.get(0), list.get(1));

        var m3u8Response = HttpUtil.get(url);

        var substring = m3u8Response.substring(5, m3u8Response.length() - 1);

        var s1 = JSONUtil.parseObj(substring);

        List<Map<String, String>> copies = (List<Map<String, String>>) s1.get("copies");

        return copies.get(0).get("playurl");
    }

    /**
     * 下载
     * @param url
     * @param pageName
     * @param videoName
     */
    public static void downloadVideo(String url, String pageName, String videoName) {

        //过滤页面非法字符
        pageName = pageName.replace("-", "").replace("/", "").replaceAll("\\s*", "");

        videoName = videoName.replace("-", "").replace("/", "").replaceAll("\\s*", "");

        String finalPageName = pageName;

        String finalVideoName = videoName;

        //线程池开启多线程
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                String path = System.getProperty("user.dir");


                path = String.format("%s%s%s%s%s", path, File.separator,"ffmpeg", File.separator,"ffmpeg.exe");

                var fileName = String.format("%s%s%s%s",videoPath, File.separator, finalPageName, File.separator);

                File file = new File(fileName);
                if (!file.exists()) {
                    file.mkdir();
                }

                //拼接cmd命令
                String cmd = String.format("%s -i %s -c copy %s", path, url, String.format("%s%s%s%s%s%s", videoPath, File.separator, finalPageName,File.separator, finalVideoName,".mp4"));

                StaticLog.info(cmd);

                //调用系统命令执行
                var s = RuntimeUtil.execForStr(Charset.defaultCharset(),cmd);

                System.out.println(s);
            }
        });
    }

    public static void downloadVideo(String url, String pageName) {

        pageName = pageName.replace("-", "").replace("/", "").replaceAll("\\s*", "");

        String finalPageName = pageName;
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                String path = System.getProperty("user.dir");


                path = String.format("%s%s%s%s%s", path, File.separator,"ffmpeg", File.separator,"ffmpeg.exe");

                var fileName = String.format("%s%s%s%s",videoPath, File.separator, finalPageName, File.separator);

                File file = new File(fileName);
                if (!file.exists()) {
                    file.mkdir();
                }else{
                    return;
                }

                String cmd = String.format("%s -i %s -c copy %s", path, url, String.format("%s%s%s%s%s%s", videoPath, File.separator, finalPageName,File.separator, finalPageName,".mp4"));

                StaticLog.info(cmd);

                var s = RuntimeUtil.execForStr(Charset.defaultCharset(),cmd);

                System.out.println(s);
            }
        });
    }

}
