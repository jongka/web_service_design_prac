package kr.ac.jbnu.bjh.web_service_design_hw1.api.request;

public class ItemPriceUpdateRequest {
    private Long id;
    private Integer price;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getPrice() { return price; }
    public void setPrice(Integer price) { this.price = price; }
}