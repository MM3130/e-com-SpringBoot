package in.mm.main.service;

import in.mm.main.exceptions.APIException;
import in.mm.main.exceptions.ResourceNotFoundException;
import in.mm.main.model.Cart;
import in.mm.main.model.CartItem;
import in.mm.main.model.Category;
import in.mm.main.model.Product;
import in.mm.main.payload.CartDTO;
import in.mm.main.payload.ProductDTO;
import in.mm.main.payload.ProductResponse;
import in.mm.main.repositories.CartRepository;
import in.mm.main.repositories.CategoryRepository;
import in.mm.main.repositories.ProductRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartService cartService;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private FileService fileService;

    @Value("${project.image}")
    private String path;

    @Override
    public ProductDTO addProduct(Long categoryId, ProductDTO productDTO) {
        Product product = modelMapper.map(productDTO, Product.class);

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(()-> new ResourceNotFoundException("Category","categoryId",categoryId));

        boolean isProductNotPresent = true;
        List<Product> products = category.getProducts();
        for(int i=0; i<products.size(); i++){
            if(products.get(i).getProductName().equalsIgnoreCase(productDTO.getProductName())){
                isProductNotPresent = false;
                break;
            }
        }
        if(isProductNotPresent){
            product.setImage("default.png");
            product.setCategory(category);
            double specialPrice = product.getPrice() - ((product.getDiscount() * 0.01) * product.getPrice());
            product.setSpecialPrice(specialPrice);

            Product savedProduct = productRepository.save(product);

            return modelMapper.map(savedProduct, ProductDTO.class);
        }else {
            throw new APIException("Product already exists");
        }
    }

    @Override
    public ProductResponse getAllProducts(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {

        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc")?Sort.by(sortBy).ascending():Sort.by(sortBy).descending();

        Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);

        Page<Product> productsPage = productRepository.findAll(pageDetails);

        List<Product> products = productsPage.getContent();
        if(products.isEmpty()) {
            throw new APIException("No Product Exist!!");
        }

        List<ProductDTO> productDTOS = products.stream().map(product -> modelMapper.map(product, ProductDTO.class)).toList();

        ProductResponse productResponse = new ProductResponse();
        productResponse.setContent(productDTOS);
        productResponse.setPageNumber(pageNumber);
        productResponse.setPageSize(pageSize);
        productResponse.setTotalElements(productsPage.getTotalElements());
        productResponse.setTotalPages(productsPage.getTotalPages());
        productResponse.setLastPage(productsPage.isLast());
        return productResponse;
    }

    @Override
    public ProductResponse searchByCategory(Long categoryId, Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(()-> new ResourceNotFoundException("Category","categoryId",categoryId));

        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc")?Sort.by(sortBy).ascending():Sort.by(sortBy).descending();

        Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);
        Page<Product> productsPage = productRepository.findByCategoryOrderByPriceAsc(category,pageDetails);

        List<Product> products = productsPage.getContent();

        if (products.isEmpty()) {
            throw new APIException(category.getCategoryName() + " does not have any products");
        }

        List<ProductDTO> productDTOS = products.stream().map(product -> modelMapper.map(product, ProductDTO.class)).toList();

        ProductResponse productResponse = new ProductResponse();
        productResponse.setContent(productDTOS);
        productResponse.setPageNumber(pageNumber);
        productResponse.setPageSize(pageSize);
        productResponse.setTotalElements(productsPage.getTotalElements());
        productResponse.setTotalPages(productsPage.getTotalPages());
        productResponse.setLastPage(productsPage.isLast());
        return productResponse;
    }

    @Override
    public ProductResponse searchProductByKeyword(String keyword, Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {

        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc")?Sort.by(sortBy).ascending():Sort.by(sortBy).descending();

        Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);
        Page<Product> productsPage = productRepository.findByProductNameLikeIgnoreCase('%'+keyword+'%',pageDetails);

        List<Product> products = productsPage.getContent();
        if (products.isEmpty()) {
            throw new APIException("No Product Not Found with keyword: "+keyword);
        }

        List<ProductDTO> productDTOS = products.stream().map(product -> modelMapper.map(product, ProductDTO.class)).toList();

        ProductResponse productResponse = new ProductResponse();
        productResponse.setContent(productDTOS);
        productResponse.setPageNumber(pageNumber);
        productResponse.setPageSize(pageSize);
        productResponse.setTotalElements(productsPage.getTotalElements());
        productResponse.setTotalPages(productsPage.getTotalPages());
        productResponse.setLastPage(productsPage.isLast());
        return productResponse;
    }

    @Override
    public ProductDTO updateProduct(Long productId, ProductDTO productDTO) {
        Product product = modelMapper.map(productDTO, Product.class);

        Product productFromDb = productRepository.findById(productId).orElseThrow(()-> new ResourceNotFoundException("Product","productId",productId));

        productFromDb.setProductName(product.getProductName());
        productFromDb.setDescription(product.getDescription());
        productFromDb.setQuantity(product.getQuantity());
        productFromDb.setPrice(product.getPrice());
        productFromDb.setDiscount(product.getDiscount());
        double specialPrice = product.getPrice() - ((product.getDiscount() * 0.01) * product.getPrice());
        productFromDb.setSpecialPrice(specialPrice);

        Product savedProduct = productRepository.save(productFromDb);

        List<Cart> carts = cartRepository.findCartsByProductId(productId);
        List<CartDTO> cartDTOS = carts.stream().map(cart -> {
            CartDTO cartDTO = modelMapper.map(cart,CartDTO.class);
            List<ProductDTO> products = cart.getCartItems().stream().map(p -> modelMapper.map(p, ProductDTO.class)).toList();
            cartDTO.setProducts(products);
            return cartDTO;
        }).toList();
        cartDTOS.forEach(cart -> cartService.updateProductInCarts(cart.getCartId(),productId));

        return modelMapper.map(savedProduct, ProductDTO.class);
    }

    @Override
    public ProductDTO deleteProduct(Long productId) {
        Product product = productRepository.findById(productId).orElseThrow(()-> new ResourceNotFoundException("Product","productId",productId));

        List<Cart> carts = cartRepository.findCartsByProductId(productId);
        carts.forEach(cart -> cartService.deleteProductFromCart(cart.getCartId(),productId));
        
        productRepository.delete(product);
        return modelMapper.map(product, ProductDTO.class);
    }

    @Override
    public ProductDTO updateProductImage(Long productId, MultipartFile image) throws IOException {
        //get Product from DB
        Product productFromDb = productRepository.findById(productId).orElseThrow(()-> new ResourceNotFoundException("Product","productId",productId));

        //Upload image to server
        //get the file name uploaded img
        String fileName = fileService.uploadImage(path,image);

        //updating the new file name to the product
        productFromDb.setImage(fileName);

        //save updated product
        Product updatedProduct = productRepository.save(productFromDb);

        //return productDTO
        return modelMapper.map(updatedProduct, ProductDTO.class);
    }
}
