package elite.vault.api.dto;

import elite.vault.util.Convertable;

import java.util.Map;

public class API_TradeRouteDto extends Convertable {

    private Map<Integer, API_TradePairDto> route;
    private String note;

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Map<Integer, API_TradePairDto> getRoute() {
        return route;
    }


    /// leg number to trade pair
    public void setRoute(Map<Integer, API_TradePairDto> route) {
        this.route = route;
    }
}
