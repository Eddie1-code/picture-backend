package com.xcw.picturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xcw.picturebackend.exception.BusinessException;
import com.xcw.picturebackend.exception.ErrorCode;
import com.xcw.picturebackend.constant.SocialRedisKey;
import com.xcw.picturebackend.exception.ThrowUtils;
import com.xcw.picturebackend.manager.PostCacheManager;
import com.xcw.picturebackend.manager.social.InteractionEvent;
import com.xcw.picturebackend.manager.social.InteractionStreamProducer;
import com.xcw.picturebackend.manager.social.RedisLockUtil;
import com.xcw.picturebackend.mapper.PostMapper;
import com.xcw.picturebackend.model.dto.post.PostAddRequest;
import com.xcw.picturebackend.model.dto.post.PostEditRequest;
import com.xcw.picturebackend.model.dto.post.PostQueryRequest;
import com.xcw.picturebackend.model.entity.Post;
import com.xcw.picturebackend.model.entity.User;
import com.xcw.picturebackend.model.enums.TargetTypeEnum;
import com.xcw.picturebackend.model.vo.PostVO;
import com.xcw.picturebackend.model.vo.UserVO;
import com.xcw.picturebackend.service.FavoriteRecordService;
import com.xcw.picturebackend.service.LikeRecordService;
import com.xcw.picturebackend.service.PostService;
import com.xcw.picturebackend.service.UserFollowService;
import com.xcw.picturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 帖子 / 动态服务实现
 * <p>
 * 设计要点：
 * - 列表采用「拉模式 feed」：直接按索引 idx_post_time 分页；单页 20 条
 * - 图片 url 不落 picture 表，仅存 imageUrls JSON
 * - 作者开关（allowLike/allowComment/allowCollect）与点赞/评论/收藏子模块配合：
 *   具体校验发生在对应子服务里（本服务仅负责字段落库与返回）
 * - 可见性规则（visibility）在「读」路径统一执行：
 *   0-公开：所有人可见
 *   1-仅粉丝：仅作者自己、作者的粉丝、管理员可见
 *   2-仅自己：仅作者、管理员可见
 */
@Slf4j
@Service
public class PostServiceImpl extends ServiceImpl<PostMapper, Post> implements PostService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_IMAGE_COUNT = 9;
    private static final int MAX_CONTENT_LEN = 5000;

    @Resource
    private UserService userService;

    @Resource
    private UserFollowService userFollowService;

    @Resource
    private LikeRecordService likeRecordService;

    @Resource
    private FavoriteRecordService favoriteRecordService;

    @Resource
    private RedisLockUtil redisLockUtil;

    @Resource
    private PostCacheManager postCacheManager;

    @Resource
    private InteractionStreamProducer interactionStreamProducer;

    // =======================================================================
    // 写操作
    // =======================================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addPost(PostAddRequest request, User loginUser) {
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);

        // 幂等锁：同一用户的同一 clientReqId，在 10s 窗口内只允许发一次
        String clientReqId = StrUtil.trimToNull(request.getClientReqId());
        if (clientReqId != null) {
            String key = SocialRedisKey.postCreateLockKey(loginUser.getId(), clientReqId);
            boolean first = redisLockUtil.tryIdempotent(key, 10L, java.util.concurrent.TimeUnit.SECONDS);
            if (!first) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "请勿重复提交");
            }
        }

        String content = StrUtil.trimToEmpty(request.getContent());
        List<String> imageUrls = request.getImageUrls() != null ? request.getImageUrls() : Collections.emptyList();

        ThrowUtils.throwIf(
                StrUtil.isBlank(content) && imageUrls.isEmpty(),
                ErrorCode.PARAMS_ERROR,
                "请至少填写文字或选择图片"
        );
        ThrowUtils.throwIf(content.length() > MAX_CONTENT_LEN, ErrorCode.PARAMS_ERROR, "正文过长");
        ThrowUtils.throwIf(imageUrls.size() > MAX_IMAGE_COUNT, ErrorCode.PARAMS_ERROR, "最多上传 9 张图片");

        Post post = new Post();
        post.setUserId(loginUser.getId());
        post.setContent(StrUtil.isBlank(content) ? null : content);
        post.setImageUrls(imageUrls.isEmpty() ? null : JSONUtil.toJsonStr(imageUrls));
        if (CollUtil.isNotEmpty(request.getTags())) {
            post.setTags(JSONUtil.toJsonStr(request.getTags()));
        }
        post.setLocation(StrUtil.trimToNull(request.getLocation()));
        post.setVisibility(normalizeVisibility(request.getVisibility()));
        post.setAllowComment(toggleOrDefault(request.getAllowComment(), 1));
        post.setAllowLike(toggleOrDefault(request.getAllowLike(), 1));
        post.setAllowCollect(toggleOrDefault(request.getAllowCollect(), 1));
        post.setLikeCount(0L);
        post.setCommentCount(0L);
        post.setFavoriteCount(0L);
        post.setViewCount(0L);
        post.setShareCount(0L);
        post.setHotScore(0.0);
        // 默认直接已发布 & 审核通过；如需审核把这两个字段置为 0/0
        post.setStatus(1);
        post.setReviewStatus(1);
        post.setReviewMessage("system auto-pass");
        post.setReviewerId(0L);
        post.setReviewTime(new Date());

        boolean ok = this.save(post);
        ThrowUtils.throwIf(!ok, ErrorCode.OPERATION_ERROR, "发帖失败");

        // 新帖发布事件：给热榜一个初始分数
        interactionStreamProducer.publish(InteractionEvent.of(
                InteractionEvent.TYPE_POST_PUBLISH,
                loginUser.getId(), loginUser.getId(),
                TargetTypeEnum.POST.getValue(), post.getId()
        ));
        return post.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean editPost(PostEditRequest request, User loginUser) {
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        ThrowUtils.throwIf(request == null || request.getId() == null, ErrorCode.PARAMS_ERROR);

        Post old = this.getById(request.getId());
        ThrowUtils.throwIf(old == null, ErrorCode.NOT_FOUND_ERROR, "帖子不存在");
        ensureOwnerOrAdmin(old, loginUser);

        Post update = new Post();
        update.setId(old.getId());
        if (request.getContent() != null) {
            String c = StrUtil.trimToEmpty(request.getContent());
            ThrowUtils.throwIf(c.length() > MAX_CONTENT_LEN, ErrorCode.PARAMS_ERROR, "正文过长");
            update.setContent(c);
        }
        if (request.getImageUrls() != null) {
            List<String> imgs = request.getImageUrls();
            ThrowUtils.throwIf(imgs.size() > MAX_IMAGE_COUNT, ErrorCode.PARAMS_ERROR, "最多 9 张图");
            update.setImageUrls(imgs.isEmpty() ? "" : JSONUtil.toJsonStr(imgs));
        }
        if (request.getTags() != null) {
            update.setTags(request.getTags().isEmpty() ? "" : JSONUtil.toJsonStr(request.getTags()));
        }
        if (request.getLocation() != null) {
            update.setLocation(StrUtil.trimToNull(request.getLocation()));
        }
        if (request.getVisibility() != null) {
            update.setVisibility(normalizeVisibility(request.getVisibility()));
        }
        if (request.getAllowComment() != null) update.setAllowComment(request.getAllowComment() == 0 ? 0 : 1);
        if (request.getAllowLike() != null)    update.setAllowLike(request.getAllowLike() == 0 ? 0 : 1);
        if (request.getAllowCollect() != null) update.setAllowCollect(request.getAllowCollect() == 0 ? 0 : 1);

        boolean ok = this.updateById(update);
        if (ok) postCacheManager.invalidateDetail(old.getId());
        return ok;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deletePost(Long postId, User loginUser) {
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        ThrowUtils.throwIf(postId == null || postId <= 0, ErrorCode.PARAMS_ERROR);
        Post old = this.getById(postId);
        ThrowUtils.throwIf(old == null, ErrorCode.NOT_FOUND_ERROR, "帖子不存在");
        ensureOwnerOrAdmin(old, loginUser);
        boolean ok = this.removeById(postId);
        if (ok) postCacheManager.invalidateDetail(postId);
        return ok;
    }

    // =======================================================================
    // 读操作
    // =======================================================================

    @Override
    public PostVO getPostVO(Long postId, HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(postId == null || postId <= 0, ErrorCode.PARAMS_ERROR);

        User loginUser = tryGetLoginUser(httpServletRequest);

        // Cache-Aside：仅缓存 visibility=0 的公开帖子详情（未登录/登录都适用）
        // 空值缓存防穿透
        PostVO cached = postCacheManager.getDetail(postId);
        if (cached != null) {
            // 命中：清空视角特有字段再按当前登录用户重新计算
            cached.setIsLiked(null);
            cached.setIsFavorite(null);
            cached.setIsFollowingAuthor(null);
            fillViewerFlags(Collections.singletonList(cached), loginUser);
            return cached;
        }
        if (postCacheManager.isNullCached(postId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "帖子不存在");
        }

        Post post = this.getById(postId);
        if (post == null) {
            postCacheManager.putDetailNull(postId);
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "帖子不存在");
        }
        ensureVisible(post, loginUser);

        PostVO vo = PostVO.objToVo(post);
        fillAuthor(vo, post.getUserId());
        fillViewerFlags(Collections.singletonList(vo), loginUser);

        // 只缓存公开贴，避免私密贴被下一位访问者复用
        if (post.getVisibility() != null && post.getVisibility() == 0) {
            PostVO cacheVo = PostVO.objToVo(post);
            fillAuthor(cacheVo, post.getUserId());
            postCacheManager.putDetail(postId, cacheVo);
        }
        return vo;
    }

    @Override
    public Page<PostVO> listPostVOByPage(PostQueryRequest request, HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        long current = Math.max(1, request.getCurrent());
        long size = Math.min(MAX_PAGE_SIZE, Math.max(1, request.getPageSize() > 0 ? request.getPageSize() : DEFAULT_PAGE_SIZE));

        User loginUser = tryGetLoginUser(httpServletRequest);

        // Cache-Aside：仅对"匿名访问 + 首页（current<=2）+ 无自定义筛选"的全局 feed 缓存命中
        boolean cacheable = loginUser == null
                && current <= 2
                && request.getUserId() == null
                && StrUtil.isBlank(request.getSearchText())
                && StrUtil.isBlank(request.getTag());
        String listCacheKey = null;
        if (cacheable) {
            listCacheKey = postCacheManager.listKey(request);
            @SuppressWarnings("unchecked")
            Page<PostVO> cached = postCacheManager.getList(listCacheKey);
            if (cached != null) return cached;
        }

        LambdaQueryWrapper<Post> q = new LambdaQueryWrapper<>();
        q.eq(Post::getStatus, 1);

        Integer reviewStatus = request.getReviewStatus() != null ? request.getReviewStatus() : 1;
        q.eq(Post::getReviewStatus, reviewStatus);

        // 可见性过滤
        if (request.getUserId() != null) {
            q.eq(Post::getUserId, request.getUserId());
            applyVisibilityFilter(q, request.getUserId(), loginUser);
        } else {
            applyGlobalVisibilityFilter(q, loginUser);
        }

        if (StrUtil.isNotBlank(request.getSearchText())) {
            q.like(Post::getContent, request.getSearchText());
        }
        if (StrUtil.isNotBlank(request.getTag())) {
            // 简单 LIKE；上线后可迁移到全文索引 / RQE
            q.like(Post::getTags, request.getTag());
        }

        boolean orderByHot = "hot".equalsIgnoreCase(request.getOrderBy());
        if (orderByHot) {
            q.orderByDesc(Post::getHotScore).orderByDesc(Post::getCreateTime);
        } else {
            q.orderByDesc(Post::getCreateTime);
        }

        Page<Post> page = this.page(new Page<>(current, size), q);

        Page<PostVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<Post> records = page.getRecords();
        if (CollUtil.isEmpty(records)) {
            voPage.setRecords(Collections.emptyList());
            return voPage;
        }

        List<PostVO> voList = records.stream().map(PostVO::objToVo).collect(Collectors.toList());

        // 批量填充作者
        Set<Long> authorIds = records.stream().map(Post::getUserId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        if (!authorIds.isEmpty()) {
            Map<Long, UserVO> authorMap = userService.listByIds(authorIds).stream()
                    .collect(Collectors.toMap(User::getId, userService::getUserVO, (a, b) -> a));
            voList.forEach(v -> {
                if (v.getUserId() != null) v.setUser(authorMap.get(v.getUserId()));
            });
        }

        fillViewerFlags(voList, loginUser);

        voPage.setRecords(voList);
        if (cacheable && listCacheKey != null) {
            postCacheManager.putList(listCacheKey, voPage);
        }
        return voPage;
    }

    @Override
    public void increaseViewCount(Long postId) {
        if (postId == null || postId <= 0) return;
        this.lambdaUpdate()
                .eq(Post::getId, postId)
                .setSql("viewCount = IFNULL(viewCount,0) + 1")
                .update();
        interactionStreamProducer.publish(InteractionEvent.of(
                InteractionEvent.TYPE_POST_VIEW,
                0L, 0L, TargetTypeEnum.POST.getValue(), postId
        ));
    }

    // =======================================================================
    // 内部工具
    // =======================================================================

    private Integer normalizeVisibility(Integer v) {
        if (v == null) return 0;
        return (v == 1 || v == 2) ? v : 0;
    }

    private Integer toggleOrDefault(Integer v, int def) {
        if (v == null) return def;
        return v == 0 ? 0 : 1;
    }

    private void ensureOwnerOrAdmin(Post post, User loginUser) {
        if (!post.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权操作他人的帖子");
        }
    }

    /**
     * 单条可见性：无权访问抛异常
     */
    private void ensureVisible(Post post, User loginUser) {
        Integer v = post.getVisibility() == null ? 0 : post.getVisibility();
        if (v == 0) return;
        if (loginUser != null && userService.isAdmin(loginUser)) return;
        if (loginUser != null && loginUser.getId().equals(post.getUserId())) return;
        if (v == 2) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "此内容仅作者可见");
        }
        // v == 1（仅粉丝）
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        boolean followed = userFollowService.isFollowing(loginUser.getId(), post.getUserId());
        ThrowUtils.throwIf(!followed, ErrorCode.NO_AUTH_ERROR, "此内容仅对粉丝开放");
    }

    /**
     * 查单个作者的帖子时，按「是否本人 / 是否粉丝」过滤 visibility
     */
    private void applyVisibilityFilter(LambdaQueryWrapper<Post> q, Long authorId, User loginUser) {
        boolean self = loginUser != null && loginUser.getId().equals(authorId);
        boolean admin = loginUser != null && userService.isAdmin(loginUser);
        if (self || admin) return; // 本人 / 管理员看全量

        boolean follower = loginUser != null && userFollowService.isFollowing(loginUser.getId(), authorId);
        if (follower) {
            q.in(Post::getVisibility, 0, 1);
        } else {
            q.eq(Post::getVisibility, 0);
        }
    }

    /**
     * 全局 feed：只看 visibility=0（公开）；管理员额外可看所有
     */
    private void applyGlobalVisibilityFilter(LambdaQueryWrapper<Post> q, User loginUser) {
        if (loginUser != null && userService.isAdmin(loginUser)) return;
        q.eq(Post::getVisibility, 0);
    }

    private void fillAuthor(PostVO vo, Long authorId) {
        if (authorId == null) return;
        User u = userService.getById(authorId);
        if (u != null) {
            vo.setUser(userService.getUserVO(u));
        }
    }

    /**
     * 批量填充：当前登录用户对每条帖子的 isLiked / isFavorite / isFollowingAuthor
     */
    private void fillViewerFlags(List<PostVO> voList, User loginUser) {
        if (loginUser == null || CollUtil.isEmpty(voList)) return;
        Long viewerId = loginUser.getId();
        Set<Long> postIds = voList.stream().map(PostVO::getId).filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Set<Long> authorIds = voList.stream().map(PostVO::getUserId).filter(java.util.Objects::nonNull).collect(Collectors.toSet());

        Set<Long> likedIds = likeRecordService.listLikedTargetIds(viewerId, TargetTypeEnum.POST.getValue(), postIds);
        Set<Long> favIds = favoriteRecordService.listFavoriteTargetIds(viewerId, TargetTypeEnum.POST.getValue(), postIds);
        Set<Long> followingIds = userFollowService.getFollowingIds(viewerId);
        if (followingIds == null) followingIds = new HashSet<>();

        for (PostVO vo : voList) {
            vo.setIsLiked(likedIds != null && vo.getId() != null && likedIds.contains(vo.getId()));
            vo.setIsFavorite(favIds != null && vo.getId() != null && favIds.contains(vo.getId()));
            vo.setIsFollowingAuthor(vo.getUserId() != null && !vo.getUserId().equals(viewerId)
                    && followingIds.contains(vo.getUserId()));
        }
    }

    private User tryGetLoginUser(HttpServletRequest request) {
        if (request == null) return null;
        try {
            return userService.getLoginUser(request);
        } catch (Exception e) {
            return null;
        }
    }
}
