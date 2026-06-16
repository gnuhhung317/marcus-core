package io.marcus.domain.model;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class PagedResult<T> {
    private final List<T> content;
    private final long totalElements;
    private final int number;
    private final int size;

    public PagedResult(List<T> content, long totalElements, int number, int size) {
        this.content = content == null ? List.of() : List.copyOf(content);
        this.totalElements = Math.max(0L, totalElements);
        this.number = Math.max(0, number);
        this.size = Math.max(1, size);
    }

    public List<T> getContent() {
        return content;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public int getNumber() {
        return number;
    }

    public int getSize() {
        return size;
    }

    public boolean hasNext() {
        return (long) (number + 1) * size < totalElements;
    }

    public <U> PagedResult<U> map(Function<? super T, U> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        return new PagedResult<>(content.stream().map(mapper).toList(), totalElements, number, size);
    }
}
