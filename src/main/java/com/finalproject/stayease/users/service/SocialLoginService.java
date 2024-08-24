package com.finalproject.stayease.users.service;

import com.finalproject.stayease.auth.model.dto.SocialLoginRequest;
import com.finalproject.stayease.auth.model.dto.SocialLoginResponse;
import com.finalproject.stayease.users.entity.SocialLogin;
import com.finalproject.stayease.users.entity.User;
import com.finalproject.stayease.users.entity.User.UserType;
import java.util.Optional;

public interface SocialLoginService {

  User registerOAuth2User(SocialLoginRequest request);

  // Region - quarantine (delete if by the end not needed)
  SocialLoginResponse socialLogin(SocialLoginRequest request, UserType userType);
  Optional<SocialLogin> findByKey(String provider, String providerUserId);
}
