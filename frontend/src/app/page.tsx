'use client';

import { useEffect, useState, useCallback } from 'react';
import { useSearchParams } from 'next/navigation';
import axios from 'axios';
import { jwtDecode } from 'jwt-decode'; // ESM 방식으로 임포트 변경

// JWT 디코딩 시 사용될 타입
interface JwtPayload {
  exp: number; // 만료 시간 (Unix timestamp)
  sub: string;
}

export default function LoginPage() {
  const searchParams = useSearchParams();

  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [userInfo, setUserInfo] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [remainingTime, setRemainingTime] = useState(0);
  const [timerInterval, setTimerInterval] = useState<NodeJS.Timeout | null>(null);
  const [tokenError, setTokenError] = useState<string | null>(null); // 토큰 에러 추적을 위한 상태 추가
  const [timeOffset, setTimeOffset] = useState(0); // 서버-클라이언트 시간 차이

  const success = searchParams.get('success');
  const error = searchParams.get('error');
  const logout = searchParams.get('logout');

  // 디버깅 도우미 함수 추가
  const logTokenInfo = (token: string | null | undefined) => {
    if (!token) {
      console.log('토큰이 없습니다.');
      return;
    }

    console.log('토큰 길이:', token.length);
    console.log('토큰 앞부분:', token.substring(0, 20) + '...');

    // 토큰 파트 분석
    const parts = token.split('.');
    console.log('토큰 파트 수:', parts.length);

    try {
      const decoded = jwtDecode<JwtPayload>(token);
      console.log('디코딩된 토큰:', decoded);
      console.log('만료 시간(exp):', decoded.exp);
      console.log('현재 시간:', Date.now() / 1000);
      console.log('남은 시간(초):', decoded.exp - Date.now() / 1000);
    } catch (e) {
      console.error('디버깅 중 토큰 디코딩 실패', e);
    }
  };

  // 남은 시간을 업데이트하는 함수
  const updateRemainingTime = useCallback(() => {
    const token = localStorage.getItem('accessToken');
    if (!token) {
      setRemainingTime(0);
      return;
    }

    try {
      // 디버깅을 위한 토큰 정보 출력
      // logTokenInfo(token);

      const decoded = jwtDecode<JwtPayload>(token);
      // 클라이언트와 서버 간의 시간 차이를 고려
      const now = (Date.now() / 1000) + timeOffset;
      const diff = decoded.exp - now;

      // 디버깅용 로그
      if (diff <= 0) {
        console.log('토큰 만료됨:', {
          expTime: decoded.exp,
          currentTime: now,
          diff: diff,
          timeOffset: timeOffset
        });
      }

      setRemainingTime(Math.max(0, Math.floor(diff)));
      setTokenError(null); // 성공 시 에러 상태 초기화
    } catch (e) {
      console.error('토큰 디코딩 에러', e);
      setTokenError(e instanceof Error ? e.message : '알 수 없는 에러');
      setRemainingTime(0);
      // 에러가 발생해도 즉시 로그아웃하지 않음
    }
  }, [timeOffset]);

  // 토큰 만료 확인 로직 개선
  useEffect(() => {
    // 토큰 에러가 있거나 로그인 상태가 아니면 실행하지 않음
    if (tokenError || !isLoggedIn) return;

    // 남은 시간이 0이면 추가 검증
    if (remainingTime === 0) {
      const token = localStorage.getItem('accessToken');
      if (!token) return;

      try {
        const decoded = jwtDecode<JwtPayload>(token);
        const now = (Date.now() / 1000) + timeOffset;

        // 토큰이 정말 만료되었는지 다시 확인
        if (decoded.exp > now) {
          console.log('토큰이 아직 유효함, 시간 재설정');
          updateRemainingTime();
          return;
        }

        console.log('토큰 실제 만료 확인됨, 로그아웃 실행');
        handleLogout();
      } catch (e) {
        console.error('최종 만료 확인 중 오류', e);
      }
    }
  }, [remainingTime, isLoggedIn, tokenError, updateRemainingTime]);

  // 토큰 만료 10초 전에 자동으로 갱신 시도
  useEffect(() => {
    if (isLoggedIn && remainingTime > 0 && remainingTime < 10) {
      console.log('토큰 만료 임박, 자동 갱신 시도');
      handleRefresh();
    }
  }, [remainingTime, isLoggedIn]);

  // 로그인 상태일 때 1초마다 남은 시간을 업데이트하는 타이머 설정
  useEffect(() => {
    if (isLoggedIn) {
      updateRemainingTime(); // 초기 업데이트
      const interval = setInterval(updateRemainingTime, 1000);
      setTimerInterval(interval);
      return () => clearInterval(interval);
    }
  }, [isLoggedIn, updateRemainingTime]);

  // URL에 accessToken, refreshToken이 있다면 저장하고, 없으면 재발급 시도
  useEffect(() => {
    const urlParams = new URLSearchParams(window.location.search);
    const accessToken = urlParams.get('accessToken');
    const refreshToken = urlParams.get('refreshToken');

    // 서버와 클라이언트 시간 동기화 시도
    fetchServerTime();

    if (accessToken && refreshToken) {
      // 토큰에 공백이 있을 경우를 대비해 저장 전 trim
      localStorage.setItem('accessToken', accessToken.trim());
      localStorage.setItem('refreshToken', refreshToken.trim());
      window.history.replaceState({}, document.title, '/');
      checkLoginStatus();
    } else {
      // 저장된 리프레시 토큰이 있다면 토큰 재발급 요청
      const storedRefreshToken = localStorage.getItem('refreshToken');
      if (storedRefreshToken) {
        axios
            .post('http://localhost:8080/api/refresh', null, {
              headers: { 'Refresh-Token': storedRefreshToken },
            })
            .then((res) => {
              const { accessToken, refreshToken } = res.data;
              localStorage.setItem('accessToken', accessToken.trim());
              localStorage.setItem('refreshToken', refreshToken.trim());
              checkLoginStatus();
            })
            .catch((err) => {
              console.error('토큰 재발급 실패', err);
              localStorage.removeItem('accessToken');
              localStorage.removeItem('refreshToken');
              checkLoginStatus();
            });
      } else {
        checkLoginStatus();
      }
    }

    if (success === 'true') {
      console.log('로그인 성공!');
      window.history.replaceState({}, document.title, '/');
    } else if (error === 'true') {
      console.log('로그인 실패!');
      window.history.replaceState({}, document.title, '/');
    } else if (logout === 'true') {
      console.log('로그아웃 완료!');
      window.history.replaceState({}, document.title, '/');
    }
  }, [success, error, logout]);

  // 서버 시간 가져오기 (시간 동기화를 위한 새로운 함수)
  const fetchServerTime = async () => {
    try {
      // 서버 시간을 가져오는 API 엔드포인트
      const response = await axios.get('http://localhost:8080/api/server-time');
      const serverTime = response.data.timestamp / 1000; // 초 단위로 변환
      const clientTime = Date.now() / 1000;
      const offset = serverTime - clientTime;

      console.log('시간 오프셋 계산:', { serverTime, clientTime, offset });
      setTimeOffset(offset);
    } catch (err) {
      console.error('서버 시간 가져오기 실패:', err);
      // 오류 시 오프셋 0으로 설정 (클라이언트 시간 사용)
      setTimeOffset(0);
    }
  };

  // 로그인 상태 확인 API 호출
  const checkLoginStatus = async () => {
    setLoading(true);
    try {
      const token = localStorage.getItem('accessToken');
      if (!token) throw new Error('토큰 없음');

      // 토큰 디버깅
      logTokenInfo(token);

      const response = await axios.get('http://localhost:8080/api/user', {
        headers: { Authorization: `Bearer ${token}` },
      });
      if (response.status === 200) {
        setIsLoggedIn(true);
        setUserInfo(response.data);
      }
    } catch (err) {
      console.log('로그인 상태 아님:', err);
      setIsLoggedIn(false);
      setUserInfo(null);

      // API 호출 실패 시 토큰 검증
      const token = localStorage.getItem('accessToken');
      if (token) {
        try {
          const decoded = jwtDecode<JwtPayload>(token);
          const now = (Date.now() / 1000) + timeOffset;

          console.log('토큰 검증 결과:', {
            exp: decoded.exp,
            now,
            isValid: decoded.exp > now
          });

          // 토큰이 만료된 경우 로컬 스토리지 정리
          if (decoded.exp <= now) {
            localStorage.removeItem('accessToken');
            localStorage.removeItem('refreshToken');
          }
        } catch (e) {
          console.error('토큰 검증 중 오류:', e);
          localStorage.removeItem('accessToken');
          localStorage.removeItem('refreshToken');
        }
      }
    } finally {
      setLoading(false);
    }
  };

  const handleKakaoLogin = () => {
    window.location.href =
        'http://localhost:8080/oauth2/authorization/kakao?prompt=login';
  };

  // 로그아웃 처리
  const handleLogout = async () => {
    try {
      const token = localStorage.getItem('accessToken');
      if (token) {
        await axios.post(
            'http://localhost:8080/api/logout',
            {},
            {
              headers: {
                Authorization: `Bearer ${token}`,
              },
            }
        );
      }
    } catch (err) {
      console.error('로그아웃 중 오류 발생:', err);
    } finally {
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      setIsLoggedIn(false);
      setUserInfo(null);
      setTokenError(null);
      if (timerInterval) clearInterval(timerInterval);
      window.location.href = '/?logout=true';
    }
  };

  // 토큰 재발급 요청 (리프레시 토큰 사용)
  const handleRefresh = async () => {
    const storedRefreshToken = localStorage.getItem('refreshToken');
    if (storedRefreshToken) {
      try {
        const res = await axios.post('http://localhost:8080/api/refresh', null, {
          headers: { 'Refresh-Token': storedRefreshToken },
        });
        const { accessToken, refreshToken } = res.data;
        localStorage.setItem('accessToken', accessToken.trim());
        localStorage.setItem('refreshToken', refreshToken.trim());

        // 새 토큰 디버깅
        console.log('토큰 갱신 성공');
        logTokenInfo(accessToken);

        updateRemainingTime();
        // 성공적으로 갱신 후 사용자 정보도 다시 불러오기
        await checkLoginStatus();
      } catch (err) {
        console.error('토큰 갱신 실패', err);

        // 갱신 실패 시 로그아웃 처리
        if (err.response && (err.response.status === 401 || err.response.status === 403)) {
          console.log('리프레시 토큰 만료, 로그아웃 필요');
          handleLogout();
        }
      }
    }
  };

  if (loading) {
    return (
        <div className="flex min-h-screen items-center justify-center">
          <p>로딩 중...</p>
        </div>
    );
  }

  return (
      <div className="flex min-h-screen flex-col items-center justify-center p-4 bg-gray-50">
        <div className="w-full max-w-md p-8 space-y-8 bg-white rounded-lg shadow-md">
          <div className="text-center">
            <h1 className="text-3xl font-bold text-gray-900">
              {isLoggedIn ? '로그인 성공!' : '로그인'}
            </h1>
            <p className="mt-2 text-gray-600">
              {isLoggedIn ? '환영합니다!' : '소셜 계정으로 간편하게 로그인하세요.'}
            </p>
          </div>

          {!isLoggedIn ? (
              <div className="mt-8 space-y-6">
                <button
                    onClick={handleKakaoLogin}
                    className="w-full flex items-center justify-center px-4 py-3 border border-transparent text-base font-medium rounded-md text-black bg-yellow-400 hover:bg-yellow-500"
                >
                  카카오로 로그인
                </button>
              </div>
          ) : (
              <div className="mt-8 space-y-4">
                {userInfo && (
                    <div className="p-4 bg-gray-100 rounded-md">
                      <p className="text-sm text-gray-700">
                        <strong>사용자 ID:</strong> {userInfo.id}
                      </p>
                      {userInfo.nickname && (
                          <p className="text-sm text-gray-700">
                            <strong>이름:</strong> {userInfo.nickname}
                          </p>
                      )}
                      {userInfo.email && (
                          <p className="text-sm text-gray-700">
                            <strong>이메일:</strong> {userInfo.email}
                          </p>
                      )}
                    </div>
                )}

                <div className="flex items-center justify-between">
                  <p className="text-sm text-gray-700">
                    남은 시간: {remainingTime}초
                  </p>
                  <button
                      onClick={handleRefresh}
                      className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700"
                  >
                    토큰 갱신
                  </button>
                </div>

                {tokenError && (
                    <div className="p-2 bg-orange-100 text-orange-800 rounded text-sm">
                      토큰 디코딩 오류: {tokenError}
                      <button
                          onClick={handleRefresh}
                          className="ml-2 px-2 py-1 bg-orange-200 text-orange-900 rounded-md text-xs"
                      >
                        재갱신 시도
                      </button>
                    </div>
                )}

                <button
                    onClick={handleLogout}
                    className="w-full px-4 py-2 border border-transparent text-base font-medium rounded-md text-white bg-red-600 hover:bg-red-700"
                >
                  로그아웃
                </button>
              </div>
          )}

          {success === 'true' && (
              <div className="mt-4 p-2 bg-green-100 text-green-800 rounded">
                로그인이 성공적으로 완료되었습니다.
              </div>
          )}
          {error === 'true' && (
              <div className="mt-4 p-2 bg-red-100 text-red-800 rounded">
                로그인 중 오류가 발생했습니다.
              </div>
          )}
          {logout === 'true' && (
              <div className="mt-4 p-2 bg-blue-100 text-blue-800 rounded">
                로그아웃이 완료되었습니다.
              </div>
          )}
        </div>
      </div>
  );
}