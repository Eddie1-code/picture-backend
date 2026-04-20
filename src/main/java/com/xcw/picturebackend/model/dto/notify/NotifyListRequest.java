package com.xcw.picturebackend.model.dto.notify;

import com.xcw.picturebackend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
public class NotifyListRequest extends PageRequest implements Serializable {

    /**
     * like / comment / favorite / follow / system
     */
    private String notifyType;

    /**
     * 是否只看未读
     */
    private Boolean onlyUnread;

    private static final long serialVersionUID = 1L;
}
