package com.lwl.travelassistant.service;

import com.lwl.travelassistant.model.Attraction;
import com.lwl.travelassistant.model.Location;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class TravelKnowledgeService {

    public List<Attraction> findAttractions(String cityName) {
        String city = normalize(cityName);
        List<Attraction> attractions = new ArrayList<>();

        if (city.contains("杭州")) {
            attractions.add(buildAttraction("西湖", "杭州西湖景区", 120.1551, 30.2741, 180, "经典必去，适合慢节奏步行和拍照", "自然风景", 4.8, 60));
            attractions.add(buildAttraction("灵隐寺", "杭州西湖区灵隐路", 120.1045, 30.2417, 120, "适合安静体验和人文游览", "人文历史", 4.7, 45));
            attractions.add(buildAttraction("河坊街", "杭州上城区河坊街", 120.1712, 30.2429, 90, "适合夜游和本地小吃体验", "美食购物", 4.5, 0));
            attractions.add(buildAttraction("西溪湿地", "杭州余杭区天目山路", 120.0635, 30.2666, 180, "适合轻松散步和避开高密度人流", "自然风景", 4.6, 80));
            return attractions;
        }

        if (city.contains("北京")) {
            attractions.add(buildAttraction("故宫", "北京东城区景山前街4号", 116.3970, 39.9180, 180, "适合第一次到访，历史内容非常集中", "人文历史", 4.9, 60));
            attractions.add(buildAttraction("什刹海", "北京西城区什刹海景区", 116.3852, 39.9430, 120, "适合傍晚散步和体验老北京氛围", "城市漫游", 4.6, 0));
            attractions.add(buildAttraction("簋街", "北京东城区东直门内大街", 116.4342, 39.9418, 90, "适合晚上安排美食体验", "美食购物", 4.4, 0));
            attractions.add(buildAttraction("颐和园", "北京海淀区新建宫门路19号", 116.2730, 39.9997, 180, "景观舒展，适合半天慢游", "自然风景", 4.8, 30));
            return attractions;
        }

        if (city.contains("成都")) {
            attractions.add(buildAttraction("宽窄巷子", "成都青羊区宽巷子", 104.0496, 30.6662, 120, "适合第一次到访和城市气氛体验", "城市漫游", 4.6, 0));
            attractions.add(buildAttraction("武侯祠", "成都武侯区武侯祠大街231号", 104.0436, 30.6419, 120, "适合历史文化偏好的用户", "人文历史", 4.7, 50));
            attractions.add(buildAttraction("锦里", "成都武侯区锦里中路", 104.0461, 30.6404, 90, "适合安排夜间小吃和休闲购物", "美食购物", 4.5, 0));
            attractions.add(buildAttraction("都江堰", "成都都江堰市公园路", 103.6487, 31.0028, 180, "适合做一日延伸路线", "自然风景", 4.8, 80));
            return attractions;
        }

        attractions.add(buildAttraction("城市地标", cityName + "核心景点", 120.0000, 30.0000, 120, "适合第一次到访，快速建立城市印象", "城市漫游", 4.4, 30));
        attractions.add(buildAttraction("本地美食街区", cityName + "热门商圈", 120.0100, 30.0050, 90, "适合晚上安排，补充城市烟火气", "美食购物", 4.3, 0));
        attractions.add(buildAttraction("休闲公园", cityName + "城市公园", 120.0200, 30.0100, 120, "适合慢节奏行程和放松", "自然风景", 4.5, 20));
        attractions.add(buildAttraction("文化展馆", cityName + "文化区域", 120.0300, 30.0150, 90, "适合补充本地历史与城市背景", "人文历史", 4.2, 40));
        return attractions;
    }

    private Attraction buildAttraction(String name,
                                       String address,
                                       double longitude,
                                       double latitude,
                                       int visitDuration,
                                       String description,
                                       String category,
                                       double rating,
                                       int ticketPrice) {
        return new Attraction(
                name,
                address,
                new Location(longitude, latitude),
                visitDuration,
                description,
                category,
                rating,
                null,
                ticketPrice
        );
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
    }
}
