package elite.vault.api.dto;

import elite.vault.util.Convertable;

import java.util.Map;

public class API_TradeRouteDto extends Convertable {

    private Map<Integer, API_TradePairDto> route;
    private String timeToComplete;

    public String getTimeToComplete() {
        return timeToComplete;
    }

    public void setTimeToComplete(String timeToComplete) {
        this.timeToComplete = timeToComplete;
    }

    public Map<Integer, API_TradePairDto> getRoute() {
        return route;
    }


    /// leg number to trade pair
    public void setRoute(Map<Integer, API_TradePairDto> route) {
        this.route = route;
    }
}
