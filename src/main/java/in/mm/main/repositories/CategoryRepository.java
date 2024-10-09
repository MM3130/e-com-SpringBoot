package in.mm.main.repositories;

import in.mm.main.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<Category,Long> {

    Category findByCategoryNameIgnoreCase(String categoryName);

}
