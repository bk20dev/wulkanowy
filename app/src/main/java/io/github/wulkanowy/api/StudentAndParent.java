package io.github.wulkanowy.api;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.github.wulkanowy.api.login.LoginErrorException;

public class StudentAndParent extends Vulcan {

    private String startPageUrl = "https://uonetplus.vulcan.net.pl/{symbol}/Start.mvc/Index";

    private String baseUrl = "https://uonetplus-opiekun.vulcan.net.pl/{symbol}/{ID}/";

    private String gradesPageUrl = baseUrl + "Oceny/Wszystkie";

    private String symbol = "";

    private String id = "";

    public StudentAndParent(Cookies cookies, String locID) throws IOException, LoginErrorException {
        this.cookies = cookies;
        this.symbol = locID;

        // get link to uonetplus-opiekun.vulcan.net.pl module
        Document startPage = getPageByUrl(startPageUrl.replace("{symbol}", symbol));
        Element studentTileLink = startPage.select(".panel.linkownia.pracownik.klient > a").first();
        String uonetPlusOpiekunUrl = studentTileLink.attr("href");

        //get context module cookie
        Connection.Response res = Jsoup.connect(uonetPlusOpiekunUrl)
                .followRedirects(true)
                .cookies(getCookies())
                .execute();

        cookies.addItems(res.cookies());

        this.id = getCalculatedID(uonetPlusOpiekunUrl);
        this.baseUrl = baseUrl
                .replace("{symbol}", getSymbol())
                .replace("{ID}", getId());
    }

    public String getGradesPageUrl() {
        return gradesPageUrl;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getId() {
        return id;
    }

    public String getCalculatedID(String uonetPlusOpiekunUrl) throws LoginErrorException {
        String[] path = uonetPlusOpiekunUrl.split("vulcan.net.pl/")[1].split("/");

        if (4 != path.length) {
            throw new LoginErrorException();
        }

        return path[1];
    }

    public String getRowDataChildValue(Element e, int index) {
        return e.select(".daneWiersz .wartosc").get(index - 1).text();
    }

    public Document getSnPPageDocument(String url) throws IOException {
        return getPageByUrl(baseUrl + url);
    }

    public List<Semester> getSemesters() throws IOException {
        return getSemesters(getSnPPageDocument(getGradesPageUrl()));
    }

    public List<Semester> getSemesters(Document gradesPage) {
        Elements semesterOptions = gradesPage.select("#okresyKlasyfikacyjneDropDownList option");

        List<Semester> semesters = new ArrayList<>();

        for (Element e : semesterOptions) {
            Semester semester = new Semester()
                    .setId(e.text())
                    .setNumber(e.attr("value"));

            if ("selected".equals(e.attr("selected"))) {
                semester.setCurrent(true);
            }

            semesters.add(semester);
        }

        return semesters;
    }

    public Semester getCurrentSemester(List<Semester> semesterList) {
        Semester current = null;
        for (Semester s : semesterList) {
            if (s.isCurrent()) {
                current = s;
                break;
            }
        }

        return current;
    }
}
