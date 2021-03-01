package net.xdclass.service;

import net.xdclass.request.CartItemRequest;
import net.xdclass.vo.CartVO;

public interface CartService {
    void addCart(CartItemRequest cartItemRequest);

    void clear();

    CartVO findMyCart();

    void deleteItem(long productId);

    void changeItemNum(CartItemRequest cartItemRequest);
}
