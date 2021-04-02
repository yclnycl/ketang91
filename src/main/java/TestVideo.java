import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;

import java.util.List;
import java.util.Map;

public class TestVideo {

    public static void main(String[] args) {

        String url = "https://p.bokecc.com/servlet/getvideofile?vid=4455D2EF39CE22339C33DC5901307461&siteid=9DD53F70C341DB9B";

        var s = HttpUtil.get(url);

        var substring = s.substring(5, s.length() - 1);

        var s1 = JSONUtil.parseObj(substring);

        List<Map<String, String>> copies = (List<Map<String, String>>) s1.get("copies");


        var playurl = copies.get(0).get("playurl");

        System.out.println(playurl);

    }
}
