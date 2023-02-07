package jpabasic.jpa.domain;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Category {

    @Id @GeneratedValue
    private Long id;

    private String name;

    @ManyToOne
    @JoinColumn(name = "UPPER_CATEGORY_ID")
    private Category upperCategory;

    @OneToMany(mappedBy = "upperCategory")
    private List<Category> lowerCategories = new ArrayList<>();

    @ManyToMany // 이건 예시일 뿐. 실무에서는 사용하지 말자
    @JoinTable(name = "CATEGORY_ITEM",
            joinColumns = @JoinColumn(name = "CATEGORY_ID"),
            inverseJoinColumns = @JoinColumn(name = "ITEM_ID")
    )
    private List<Item> items = new ArrayList<>();

}
