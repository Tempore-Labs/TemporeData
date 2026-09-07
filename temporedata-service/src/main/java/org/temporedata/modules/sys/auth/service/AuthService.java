package org.temporedata.modules.sys.auth.service;

import org.temporedata.api.base.exceptions.BusinessException;
import org.temporedata.api.sys.auth.LoginReq;
import org.temporedata.api.sys.auth.LoginRes;
import org.temporedata.api.sys.auth.UserInfoRes;
import org.temporedata.modules.dev.perm.service.PermissionService;
import org.temporedata.modules.sys.role.entity.RoleEntity;
import org.temporedata.modules.sys.role.entity.UserRoleEntity;
import org.temporedata.modules.sys.role.repository.RoleRepository;
import org.temporedata.modules.sys.role.repository.UserRoleRepository;
import org.temporedata.modules.sys.user.entity.UserEntity;
import org.temporedata.modules.sys.user.repository.UserRepository;
import org.temporedata.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final PermissionService permissionService;

    /**
     * Authenticate user with username & password, return JWT token + user info
     * including resolved roles and permission points (identity <-> authorization).
     */
    @Transactional(readOnly = true)
    public LoginRes login(LoginReq req) {
        UserEntity user = userRepository.findByUsername(req.getUsername())
                .orElseThrow(() -> new BusinessException("用户名或密码错误"));

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        if (user.isDisabled()) {
            throw new BusinessException("账户已被禁用");
        }

        String token = jwtTokenProvider.generateToken(user.getId(), user.getTenantId(), user.getUsername());

        List<String> roleIds = resolveRoleIds(user.getId());
        List<String> roleNames = roleIds.stream()
                .map(roleRepository::findById)
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .map(RoleEntity::getCode)
                .collect(Collectors.toList());
        boolean superAdmin = roleIds.stream()
                .map(roleRepository::findById)
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                // Compare against the seeded role code `ROLE_ADMIN` (V1__base_schema.sql),
                // not the bare `admin`, so the admin flag actually resolves for real admins.
                .anyMatch(r -> "ROLE_ADMIN".equalsIgnoreCase(r.getCode()));
        List<String> permissions = permissionService.myPerms(roleIds);

        UserInfoRes userInfo = new UserInfoRes();
        userInfo.setUserId(user.getId());
        userInfo.setUsername(user.getUsername());
        userInfo.setTenantId(user.getTenantId());
        userInfo.setTenantName(user.getTenantId());
        userInfo.setRoles(roleNames);
        userInfo.setPermissions(permissions);
        userInfo.setAdmin(superAdmin);
        log.info("User {} resolved {} roles, {} permission points (superAdmin={})",
                user.getUsername(), roleNames.size(), permissions.size(), superAdmin);

        return new LoginRes(token, userInfo);
    }

    /**
     * Resolve the role ids bound to the given user from zy_user_role.
     */
    private List<String> resolveRoleIds(String userId) {
        List<UserRoleEntity> links = userRoleRepository.findByIdUserId(userId);
        List<String> ids = new ArrayList<>();
        for (UserRoleEntity link : links) {
            if (link.getId() != null && link.getId().getRoleId() != null) {
                ids.add(link.getId().getRoleId());
            }
        }
        return ids;
    }

    /**
     * Logout: stateless JWT, client-side token discard.
     */
    public void logout() {
        log.debug("User logged out");
    }
}