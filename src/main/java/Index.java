import cn.hutool.http.HttpRequest;
import cn.hutool.log.StaticLog;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.*;

public class Index {

    public static Set<String> list = new HashSet<>();

    public static Map<String, String> cookie = new HashMap<>(){{
        put("Cookie", "uniqueVisitorId=c6ecaabc-27de-ce32-4d88-702cbdcf0527; Hm_lvt_80dc68f625454799fff627010e77e7df=1615952235,1617361683; XSRF-TOKEN=eyJpdiI6InNQYkVYeDhQeFJaVGtWVUkzSlA1UEE9PSIsInZhbHVlIjoiODRSOEZ6cUVZSjNCQU4rZFErWHA1cXNYb1dudVBQSGNGanhvbUVGVTNVK0hxY211ajF2XC9aeHhYNXlEWFwvQ1lXa2VQUFd6NE0xaG5UNnd2R0hyb1lXUT09IiwibWFjIjoiNmJhODZjNjc3NDRiNjI3NmU5YWU0ZjlhMjdhNDVhZWNhZTZkZTVkMjM1NmI1YjRiYmEyMmI1ZTNlNGY0YWFlZCJ9; laravel_session=eyJpdiI6IjBYNFR3cWxNeENQNXF6eGFxNHk1T2c9PSIsInZhbHVlIjoiR2R1eXo5WXBFUENTRmdPUzhSMnk0aDRMTjJZWmI3bitzMk5Ib3BXRExGRXVzQTd0eFlEZmVTNlhTaGJJK3BuS002d3BMd0hUU2ZwUjVRTWpOMFZTZ2c9PSIsIm1hYyI6IjFhNmYxNDMyZWQ2NmVjZmY2NzY1ZDNjODU1MTRmNDQ3YzlkOWVhNjEzYzAxNmQ4YzM1NDU5NTIwODZjMzNkNWYifQ%3D%3D; Hm_lpvt_80dc68f625454799fff627010e77e7df=1617362786");
    }};

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

    public static void getItemInfo(String url) {
        System.out.println(url);
    }

    public static void vip_single_channel_html(String boyd) {



    }

    public static void vip_channel(String boyd) {



    }

}
