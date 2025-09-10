package generator.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;

/**
 * 图片
 * @TableName picture
 */
@TableName(value ="picture")
public class Picture {
    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 图片 url
     */
    private String url;

    /**
     * 图片名称
     */
    private String name;

    /**
     * 简介
     */
    private String introduction;

    /**
     * 分类
     */
    private String category;

    /**
     * 标签（JSON 数组）
     */
    private String tags;

    /**
     * 图片体积
     */
    private Long picsize;

    /**
     * 图片宽度
     */
    private Integer picwidth;

    /**
     * 图片高度
     */
    private Integer picheight;

    /**
     * 图片宽高比例
     */
    private Double picscale;

    /**
     * 图片格式
     */
    private String picformat;

    /**
     * 创建用户 id
     */
    private Long userid;

    /**
     * 创建时间
     */
    private Date createtime;

    /**
     * 编辑时间
     */
    private Date edittime;

    /**
     * 更新时间
     */
    private Date updatetime;

    /**
     * 是否删除
     */
    private Integer isdelete;

    /**
     * 审核状态：0-待审核; 1-通过; 2-拒绝
     */
    private Integer reviewstatus;

    /**
     * 审核信息
     */
    private String reviewmessage;

    /**
     * 审核人 ID
     */
    private Long reviewerid;

    /**
     * 审核时间
     */
    private Date reviewtime;

    /**
     * id
     */
    public Long getId() {
        return id;
    }

    /**
     * id
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 图片 url
     */
    public String getUrl() {
        return url;
    }

    /**
     * 图片 url
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * 图片名称
     */
    public String getName() {
        return name;
    }

    /**
     * 图片名称
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 简介
     */
    public String getIntroduction() {
        return introduction;
    }

    /**
     * 简介
     */
    public void setIntroduction(String introduction) {
        this.introduction = introduction;
    }

    /**
     * 分类
     */
    public String getCategory() {
        return category;
    }

    /**
     * 分类
     */
    public void setCategory(String category) {
        this.category = category;
    }

    /**
     * 标签（JSON 数组）
     */
    public String getTags() {
        return tags;
    }

    /**
     * 标签（JSON 数组）
     */
    public void setTags(String tags) {
        this.tags = tags;
    }

    /**
     * 图片体积
     */
    public Long getPicsize() {
        return picsize;
    }

    /**
     * 图片体积
     */
    public void setPicsize(Long picsize) {
        this.picsize = picsize;
    }

    /**
     * 图片宽度
     */
    public Integer getPicwidth() {
        return picwidth;
    }

    /**
     * 图片宽度
     */
    public void setPicwidth(Integer picwidth) {
        this.picwidth = picwidth;
    }

    /**
     * 图片高度
     */
    public Integer getPicheight() {
        return picheight;
    }

    /**
     * 图片高度
     */
    public void setPicheight(Integer picheight) {
        this.picheight = picheight;
    }

    /**
     * 图片宽高比例
     */
    public Double getPicscale() {
        return picscale;
    }

    /**
     * 图片宽高比例
     */
    public void setPicscale(Double picscale) {
        this.picscale = picscale;
    }

    /**
     * 图片格式
     */
    public String getPicformat() {
        return picformat;
    }

    /**
     * 图片格式
     */
    public void setPicformat(String picformat) {
        this.picformat = picformat;
    }

    /**
     * 创建用户 id
     */
    public Long getUserid() {
        return userid;
    }

    /**
     * 创建用户 id
     */
    public void setUserid(Long userid) {
        this.userid = userid;
    }

    /**
     * 创建时间
     */
    public Date getCreatetime() {
        return createtime;
    }

    /**
     * 创建时间
     */
    public void setCreatetime(Date createtime) {
        this.createtime = createtime;
    }

    /**
     * 编辑时间
     */
    public Date getEdittime() {
        return edittime;
    }

    /**
     * 编辑时间
     */
    public void setEdittime(Date edittime) {
        this.edittime = edittime;
    }

    /**
     * 更新时间
     */
    public Date getUpdatetime() {
        return updatetime;
    }

    /**
     * 更新时间
     */
    public void setUpdatetime(Date updatetime) {
        this.updatetime = updatetime;
    }

    /**
     * 是否删除
     */
    public Integer getIsdelete() {
        return isdelete;
    }

    /**
     * 是否删除
     */
    public void setIsdelete(Integer isdelete) {
        this.isdelete = isdelete;
    }

    /**
     * 审核状态：0-待审核; 1-通过; 2-拒绝
     */
    public Integer getReviewstatus() {
        return reviewstatus;
    }

    /**
     * 审核状态：0-待审核; 1-通过; 2-拒绝
     */
    public void setReviewstatus(Integer reviewstatus) {
        this.reviewstatus = reviewstatus;
    }

    /**
     * 审核信息
     */
    public String getReviewmessage() {
        return reviewmessage;
    }

    /**
     * 审核信息
     */
    public void setReviewmessage(String reviewmessage) {
        this.reviewmessage = reviewmessage;
    }

    /**
     * 审核人 ID
     */
    public Long getReviewerid() {
        return reviewerid;
    }

    /**
     * 审核人 ID
     */
    public void setReviewerid(Long reviewerid) {
        this.reviewerid = reviewerid;
    }

    /**
     * 审核时间
     */
    public Date getReviewtime() {
        return reviewtime;
    }

    /**
     * 审核时间
     */
    public void setReviewtime(Date reviewtime) {
        this.reviewtime = reviewtime;
    }

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null) {
            return false;
        }
        if (getClass() != that.getClass()) {
            return false;
        }
        Picture other = (Picture) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getUrl() == null ? other.getUrl() == null : this.getUrl().equals(other.getUrl()))
            && (this.getName() == null ? other.getName() == null : this.getName().equals(other.getName()))
            && (this.getIntroduction() == null ? other.getIntroduction() == null : this.getIntroduction().equals(other.getIntroduction()))
            && (this.getCategory() == null ? other.getCategory() == null : this.getCategory().equals(other.getCategory()))
            && (this.getTags() == null ? other.getTags() == null : this.getTags().equals(other.getTags()))
            && (this.getPicsize() == null ? other.getPicsize() == null : this.getPicsize().equals(other.getPicsize()))
            && (this.getPicwidth() == null ? other.getPicwidth() == null : this.getPicwidth().equals(other.getPicwidth()))
            && (this.getPicheight() == null ? other.getPicheight() == null : this.getPicheight().equals(other.getPicheight()))
            && (this.getPicscale() == null ? other.getPicscale() == null : this.getPicscale().equals(other.getPicscale()))
            && (this.getPicformat() == null ? other.getPicformat() == null : this.getPicformat().equals(other.getPicformat()))
            && (this.getUserid() == null ? other.getUserid() == null : this.getUserid().equals(other.getUserid()))
            && (this.getCreatetime() == null ? other.getCreatetime() == null : this.getCreatetime().equals(other.getCreatetime()))
            && (this.getEdittime() == null ? other.getEdittime() == null : this.getEdittime().equals(other.getEdittime()))
            && (this.getUpdatetime() == null ? other.getUpdatetime() == null : this.getUpdatetime().equals(other.getUpdatetime()))
            && (this.getIsdelete() == null ? other.getIsdelete() == null : this.getIsdelete().equals(other.getIsdelete()))
            && (this.getReviewstatus() == null ? other.getReviewstatus() == null : this.getReviewstatus().equals(other.getReviewstatus()))
            && (this.getReviewmessage() == null ? other.getReviewmessage() == null : this.getReviewmessage().equals(other.getReviewmessage()))
            && (this.getReviewerid() == null ? other.getReviewerid() == null : this.getReviewerid().equals(other.getReviewerid()))
            && (this.getReviewtime() == null ? other.getReviewtime() == null : this.getReviewtime().equals(other.getReviewtime()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getUrl() == null) ? 0 : getUrl().hashCode());
        result = prime * result + ((getName() == null) ? 0 : getName().hashCode());
        result = prime * result + ((getIntroduction() == null) ? 0 : getIntroduction().hashCode());
        result = prime * result + ((getCategory() == null) ? 0 : getCategory().hashCode());
        result = prime * result + ((getTags() == null) ? 0 : getTags().hashCode());
        result = prime * result + ((getPicsize() == null) ? 0 : getPicsize().hashCode());
        result = prime * result + ((getPicwidth() == null) ? 0 : getPicwidth().hashCode());
        result = prime * result + ((getPicheight() == null) ? 0 : getPicheight().hashCode());
        result = prime * result + ((getPicscale() == null) ? 0 : getPicscale().hashCode());
        result = prime * result + ((getPicformat() == null) ? 0 : getPicformat().hashCode());
        result = prime * result + ((getUserid() == null) ? 0 : getUserid().hashCode());
        result = prime * result + ((getCreatetime() == null) ? 0 : getCreatetime().hashCode());
        result = prime * result + ((getEdittime() == null) ? 0 : getEdittime().hashCode());
        result = prime * result + ((getUpdatetime() == null) ? 0 : getUpdatetime().hashCode());
        result = prime * result + ((getIsdelete() == null) ? 0 : getIsdelete().hashCode());
        result = prime * result + ((getReviewstatus() == null) ? 0 : getReviewstatus().hashCode());
        result = prime * result + ((getReviewmessage() == null) ? 0 : getReviewmessage().hashCode());
        result = prime * result + ((getReviewerid() == null) ? 0 : getReviewerid().hashCode());
        result = prime * result + ((getReviewtime() == null) ? 0 : getReviewtime().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", url=").append(url);
        sb.append(", name=").append(name);
        sb.append(", introduction=").append(introduction);
        sb.append(", category=").append(category);
        sb.append(", tags=").append(tags);
        sb.append(", picsize=").append(picsize);
        sb.append(", picwidth=").append(picwidth);
        sb.append(", picheight=").append(picheight);
        sb.append(", picscale=").append(picscale);
        sb.append(", picformat=").append(picformat);
        sb.append(", userid=").append(userid);
        sb.append(", createtime=").append(createtime);
        sb.append(", edittime=").append(edittime);
        sb.append(", updatetime=").append(updatetime);
        sb.append(", isdelete=").append(isdelete);
        sb.append(", reviewstatus=").append(reviewstatus);
        sb.append(", reviewmessage=").append(reviewmessage);
        sb.append(", reviewerid=").append(reviewerid);
        sb.append(", reviewtime=").append(reviewtime);
        sb.append("]");
        return sb.toString();
    }
}