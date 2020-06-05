package me.nuguri.common.support;

import me.nuguri.common.dto.PageableCondition;
import org.springframework.validation.Errors;

import static org.springframework.util.StringUtils.hasText;

public class PaginationValidator {

    /**
     * Pagination 도메인 condition 값 중 페이지, 페이지 사이즈, 정렬 방식 검증
     *
     * @param pageableCondition page 페이지 번호, size 페이지 사이즈, sort 정렬 방식
     * @param entityType        페이징 객체 엔티티 타입 클래스
     * @param errors            에러
     * @param <T>               페이징 객체 엔티티 타입
     */
    public <T> void validate(PageableCondition pageableCondition, Class<T> entityType, Errors errors) {
        if (!pageableCondition.getPage().matches("^[1-9][0-9]*$")) {
            errors.rejectValue("page", "wrongValue", "page is wrong");
        }
        if (!pageableCondition.getSize().matches("^[1-9][0-9]*$")) {
            errors.rejectValue("size", "wrongValue", "size is wrong");
        }
        if (hasText(pageableCondition.getSort())) {
            String[] sort = pageableCondition.getSort().split(",");
            if (sort.length > 1) {
                for (int i = 0; i < sort.length - 1; i++) {
                    String property = sort[i];
                    try {
                        entityType.getDeclaredField(property);
                    } catch (NoSuchFieldException e) {
                        errors.rejectValue("sort", "wrongValue", "sort property is wrong");
                    }
                }
                if (!sort[sort.length - 1].equalsIgnoreCase("asc") && !sort[sort.length - 1].equalsIgnoreCase("desc")) {
                    errors.rejectValue("sort", "wrongValue", "sort direction is wrong");
                }
            } else if (sort.length == 1) {
                try {
                    entityType.getDeclaredField(sort[0]);
                } catch (NoSuchFieldException e) {
                    errors.rejectValue("sort", "wrongValue", "sort property is wrong");
                }
            }
        }
    }

}