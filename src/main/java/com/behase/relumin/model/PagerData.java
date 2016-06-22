package com.behase.relumin.model;

import lombok.Data;

import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

@Data
public class PagerData<T> {
    private long offset;
    private long limit;
    private long total;
    private List<T> data;

    private long currentPage;
    private long lastPage;

    public PagerData(long offset, long limit, long total, List<T> data) {
        checkArgument(offset >= 0);
        checkArgument(limit > 0);
        checkArgument(total >= 0);
        checkNotNull(data);

        this.offset = offset;
        this.limit = limit;
        this.total = total;
        this.data = data;

        currentPage = (long) Math.floor((double) offset / (double) limit) + 1;
        lastPage = (long) Math.ceil((double) total / (double) limit);
        if (lastPage == 0) {
            lastPage = 1;
        }
    }

    public long getSize() {
        return data.size();
    }

    /**
     * ひとつ前のpageがあるかどうか
     *
     * @return
     */
    public boolean hasPrevPage() {
        return hasPage(currentPage - 1);
    }

    /**
     * ひとつ後のpageがあるかどうか
     *
     * @return
     */
    public boolean hasNextPage() {
        return hasPage(currentPage + 1);
    }

    /**
     * 指定したpageがあるかどうか
     *
     * @param page
     * @return
     */
    public boolean hasPage(long page) {
        if (page <= 0) {
            return false;
        } else if (0 < page && page <= lastPage) {
            return true;
        } else {
            return false;
        }
    }

    public Long getPrevPage() {
        if (hasPrevPage()) {
            return currentPage - 1;
        }
        return null;
    }

    public Long getNextPage() {
        if (hasNextPage()) {
            return currentPage + 1;
        }
        return null;
    }
}
