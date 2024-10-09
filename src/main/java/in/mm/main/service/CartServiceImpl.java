package in.mm.main.service;

import in.mm.main.exceptions.APIException;
import in.mm.main.exceptions.ResourceNotFoundException;
import in.mm.main.model.Cart;
import in.mm.main.model.CartItem;
import in.mm.main.model.Product;
import in.mm.main.payload.CartDTO;
import in.mm.main.payload.ProductDTO;
import in.mm.main.repositories.CartItemRepository;
import in.mm.main.repositories.CartRepository;
import in.mm.main.repositories.ProductRepository;
import in.mm.main.util.AuthUtil;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Stream;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private AuthUtil authUtil;

    @Override
    public CartDTO addProductToCart(Long productId, Integer quantity) {
        //create cart if user does not have then create new, if there is an existing cart then return the cart id
        // find existing cart or create new cart
        Cart cart = createCart();
        //retrive product details
        Product product = productRepository.findById(productId).orElseThrow(
                () -> new ResourceNotFoundException("Product","productId",productId));
        //perform validation // like already present or if quatity have 5 you are adding 10 like this
        CartItem cartItem = cartItemRepository.findCartItemByProductIdAndCartId(cart.getCartId(),productId);
        if(cartItem != null) {
            throw new APIException("Product "+product.getProductName()+" already exists in the cart");
        }
        if(product.getQuantity() == 0) {
            throw new APIException(product.getProductName()+" is not available");
        }
        if(product.getQuantity()<=quantity) {
            throw new APIException("Please, make an order of the "+product.getProductName()+
                    " less than or equal to the quantity "+product.getQuantity()+".");
        }
        //create cart item
        CartItem newCartItem = new CartItem();
        newCartItem.setProduct(product);
        newCartItem.setCart(cart);
        newCartItem.setQuantity(quantity);
        newCartItem.setDiscount(product.getDiscount());
        newCartItem.setProductPrice(product.getSpecialPrice());
        //save cart item
        cartItemRepository.save(newCartItem);

        //product.setQuantity(product.getQuantity());

        // return the updated cart
        cart.setTotalPrice(cart.getTotalPrice() + (product.getSpecialPrice() * quantity));
        cartRepository.save(cart);

        CartDTO cartDTO = modelMapper.map(cart,CartDTO.class);
        List<CartItem> cartItems = cart.getCartItems();
        Stream<ProductDTO> productDTOStream = cartItems.stream().map(item -> {
            ProductDTO productDTO = modelMapper.map(item.getProduct(), ProductDTO.class);
            productDTO.setQuantity(item.getQuantity());
            return productDTO;
        });
        cartDTO.setProducts(productDTOStream.toList());
        return cartDTO;
    }

    @Override
    public List<CartDTO> getAllCarts() {
        List<Cart> carts = cartRepository.findAll();
        if(carts.size()==0){
            throw new APIException("No Cart Exists");
        }

        List<CartDTO> cartDTOS = carts.stream().map(cart -> {
            CartDTO cartDTO = modelMapper.map(cart,CartDTO.class);
            List<ProductDTO> products = cart.getCartItems().stream().map(cartItem -> {
                ProductDTO productDTO = modelMapper.map(cartItem.getProduct(), ProductDTO.class);
                productDTO.setQuantity(cartItem.getQuantity()); // set the quantity from cartitem
                return productDTO;
            }).toList();
            cartDTO.setProducts(products);
            return cartDTO;
        }).toList();
        return cartDTOS;
    }

    @Override
    public CartDTO getCart(String emailId, Long cartId) {
        Cart cart = cartRepository.findCartByEmailAndCartId(emailId,cartId);
        if (cart == null){
            throw new ResourceNotFoundException("Cart","cartId",cartId);
        }

        CartDTO cartDTO = modelMapper.map(cart,CartDTO.class);

        Stream<ProductDTO> productDTOStream = cart.getCartItems().stream().map(item -> {
            ProductDTO productDTO = modelMapper.map(item.getProduct(), ProductDTO.class);
            productDTO.setQuantity(item.getQuantity());
            return productDTO;
        });
        cartDTO.setProducts(productDTOStream.toList());
//        List<ProductDTO> products = cart.getCartItems().stream()
//                .map(p -> modelMapper.map(p.getProduct(),ProductDTO.class)).toList();
//        cartDTO.setProducts(products);
        return cartDTO;
    }

    @Transactional
    @Override
    public CartDTO updateProductQuantityInCart(Long productId, Integer quantity) {
        String emailId = authUtil.loggedInEmail();
        Cart userCart = cartRepository.findCartByEmail(emailId);
        Long cartId = userCart.getCartId();

        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart","cartId",cartId));

        Product product = productRepository.findById(productId)
                .orElseThrow(()-> new ResourceNotFoundException("Product","productId",productId));

        if(product.getQuantity() == 0) {
            throw new APIException(product.getProductName()+" is not available");
        }
        if(product.getQuantity()<=quantity) {
            throw new APIException("Please, make an order of the "+product.getProductName()+
                    " less than or equal to the quantity "+product.getQuantity()+".");
        }

        CartItem cartItem = cartItemRepository.findCartItemByProductIdAndCartId(cart.getCartId(),productId);

        if(cartItem == null) {
            throw new APIException("Product "+product.getProductName()+" not available in the cart");
        }

        // validation to prevent negative quantity
        int newQuantity = cartItem.getQuantity() + quantity;
        if(newQuantity<0){
            throw new APIException("The Resulting quantity cannot be negative");
        }
        if(newQuantity == 0) {
            deleteProductFromCart(cartId,productId);
        }else {
            cartItem.setProductPrice(product.getSpecialPrice());
            cartItem.setQuantity(cartItem.getQuantity() + quantity);
            cartItem.setDiscount(product.getDiscount());
            cart.setTotalPrice(cart.getTotalPrice() + (cartItem.getProductPrice() * quantity));
            cartRepository.save(cart);
        }
        CartItem updatedItem = cartItemRepository.save(cartItem);
        if(updatedItem.getQuantity() == 0) {
            cartItemRepository.deleteById(updatedItem.getCartItemId());
        }

        CartDTO cartDTO = modelMapper.map(cart,CartDTO.class);

        Stream<ProductDTO> productDTOStream = cart.getCartItems().stream().map(item -> {
            ProductDTO productDTO = modelMapper.map(item.getProduct(), ProductDTO.class);
            productDTO.setQuantity(item.getQuantity());
            return productDTO;
        });
        cartDTO.setProducts(productDTOStream.toList());
        return cartDTO;
    }

    @Transactional
    @Override
    public String deleteProductFromCart(Long cartId, Long productId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart","cartId",cartId));

        CartItem cartItem = cartItemRepository.findCartItemByProductIdAndCartId(cartId,productId);

        if (cartItem == null){
            throw new ResourceNotFoundException("Product","productId",productId);
        }
        cart.setTotalPrice(cart.getTotalPrice()-(cartItem.getProductPrice() * cartItem.getQuantity()));

//        Product product = cartItem.getProduct();
//        product.setQuantity(product.getQuantity() + cartItem.getQuantity());

        cartItemRepository.deleteCartItemByProductIdAndCartId(cartId,productId);
        return "Product "+cartItem.getProduct().getProductName()+" has been removed from the cart";
    }

    @Override
    public void updateProductInCarts(Long cartId, Long productId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart","cartId",cartId));

        Product product = productRepository.findById(productId)
                .orElseThrow(()-> new ResourceNotFoundException("Product","productId",productId));

        CartItem cartItem = cartItemRepository.findCartItemByProductIdAndCartId(cartId,productId);

        if (cartItem == null){
            throw new ResourceNotFoundException("Product","productId",productId);
        }

        double cartPrice = cart.getTotalPrice() - (cartItem.getProductPrice() * cartItem.getQuantity());

        cartItem.setProductPrice(product.getSpecialPrice());

        cart.setTotalPrice(cartPrice + (cartItem.getProductPrice() * cartItem.getQuantity()));

        cartItem = cartItemRepository.save(cartItem);

    }

    private Cart createCart() {
        Cart userCart = cartRepository.findCartByEmail(authUtil.loggedInEmail());
        if(userCart != null) {
            return userCart;
        }
        Cart cart = new Cart();
        cart.setTotalPrice(0.00);
        cart.setUser(authUtil.loggedInUser());
        return cartRepository.save(cart);
    }

}
