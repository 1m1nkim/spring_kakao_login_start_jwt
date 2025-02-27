'use client';

import { useEffect, useState } from 'react';
import { useSearchParams } from 'next/navigation';
import axios from 'axios';

export default function LoginPage() {
  const searchParams = useSearchParams();

  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [userInfo, setUserInfo] = useState(null);
  const [loading, setLoading] = useState(true);

  // URL 파라미터 (예: ?success=true, ?error=true 등)
  const success = searchParams.get('success');
  const error = searchParams.get('error');
  const logout = searchParams.get('logout');

  // 페이지 로드 시 실행
  useEffect(() => {
    // 1) URL에 token=... 있으면 localStorage에 저장
    const urlParams = new URLSearchParams(window.location.search);
    const token = urlParams.get('token');
    if (token) {
      localStorage.setItem('accessToken', token);
      // URL 파라미터 정리 (선택)
      window.history.replaceState({}, document.title, '/');
    }

    // 2) 로그인 상태 체크
    checkLoginStatus();

    // 3) URL 파라미터에 따른 메시지 표시 (선택)
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

  // 서버에 /api/user 요청 → JWT 인증 확인
  const checkLoginStatus = async () => {
    setLoading(true);
    try {
      const accessToken = localStorage.getItem('accessToken');
      if (!accessToken) {
        throw new Error('No token');
      }

      // JWT 토큰을 Authorization 헤더에 담아서 요청
      const response = await axios.get('http://localhost:8080/api/user', {
        headers: {
          Authorization: `Bearer ${accessToken}`
        }
      });

      if (response.status === 200) {
        setIsLoggedIn(true);
        setUserInfo(response.data);
      }
    } catch (err) {
      console.log('로그인 상태가 아님:', err);
      setIsLoggedIn(false);
      setUserInfo(null);
    } finally {
      setLoading(false);
    }
  };

  // 카카오 로그인 시작
  const handleKakaoLogin = () => {
    // prompt=login: 매번 카카오 인증창 표시
    window.location.href = 'http://localhost:8080/oauth2/authorization/kakao?prompt=login';
  };

  // 로그아웃
  const handleLogout = async () => {
    try {
      // 서버에 /logout 호출 (원한다면 Authorization 헤더 포함)
      await axios.post('http://localhost:8080/api/logout', {}, {
        headers: {
          Authorization: `Bearer ${localStorage.getItem('accessToken')}`
        }
      });

      // 로컬 스토리지에 저장된 토큰 삭제
      localStorage.removeItem('accessToken');

      // 로그아웃 상태로 전환
      setIsLoggedIn(false);
      setUserInfo(null);

      // 필요 시 페이지 리로드
      window.location.href = '/?logout=true';
    } catch (err) {
      console.error('로그아웃 중 오류 발생:', err);
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

                <button
                    onClick={handleLogout}
                    className="w-full px-4 py-2 border border-transparent text-base font-medium rounded-md text-white bg-red-600 hover:bg-red-700"
                >
                  로그아웃
                </button>
              </div>
          )}

          {/* URL 파라미터에 따른 메시지 표시 (선택) */}
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
