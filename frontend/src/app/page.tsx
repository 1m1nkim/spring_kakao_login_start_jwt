'use client';

import { useEffect, useState } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import axios from 'axios';

export default function LoginPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [userInfo, setUserInfo] = useState(null);
  const [loading, setLoading] = useState(true);

  // URL 파라미터 체크
  const success = searchParams.get('success');
  const error = searchParams.get('error');
  const logout = searchParams.get('logout');

  // 컴포넌트 마운트 시 로그인 상태 확인
  useEffect(() => {
    checkLoginStatus();

    // URL 파라미터를 확인하고 메시지 표시 (필요시)
    if (success === 'true') {
      console.log('로그인 성공!');
      // 파라미터 제거를 위한 URL 리셋 (선택사항)
      window.history.replaceState({}, document.title, '/');
    } else if (error === 'true') {
      console.log('로그인 실패!');
      window.history.replaceState({}, document.title, '/');
    } else if (logout === 'true') {
      console.log('로그아웃 완료!');
      window.history.replaceState({}, document.title, '/');
    }
  }, [success, error, logout]);

  // 로그인 상태 확인 함수
  const checkLoginStatus = async () => {
    setLoading(true);
    try {
      const response = await axios.get('http://localhost:8080/api/user', {
        withCredentials: true // 쿠키를 포함하여 요청 전송
      });

      if (response.status === 200) {
        setIsLoggedIn(true);
        setUserInfo(response.data);
      }
    } catch (error) {
      console.log('로그인 상태가 아닙니다.');
      setIsLoggedIn(false);
      setUserInfo(null);
    } finally {
      setLoading(false);
    }
  };

  // 카카오 로그인 시작
  const handleKakaoLogin = () => {
    // 매번 인증 화면을 보여주도록 prompt=login 파라미터 추가
    window.location.href = 'http://localhost:8080/oauth2/authorization/kakao?prompt=login';
  };

  // 직접 로그아웃 처리
  const handleLogout = async () => {
    try {
      await axios.post('http://localhost:8080/api/logout', {}, {
        withCredentials: true
      });

      // 로그아웃 후 상태 업데이트
      setIsLoggedIn(false);
      setUserInfo(null);

      // 쿠키 삭제를 확실히 하기 위해 페이지 새로고침
      window.location.href = '/?logout=true';
    } catch (error) {
      console.error('로그아웃 중 오류 발생:', error);
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
                    className="w-full flex items-center justify-center px-4 py-3 border border-transparent text-base font-medium rounded-md text-black bg-yellow-400 hover:bg-yellow-500 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-yellow-500"
                >
                  <div className="flex items-center">
                    <span className="text-black">카카오로 로그인</span>
                  </div>
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
                    className="w-full px-4 py-2 border border-transparent text-base font-medium rounded-md text-white bg-red-600 hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-red-500"
                >
                  로그아웃
                </button>
              </div>
          )}

          {/* URL 파라미터에 따른 메시지 표시 */}
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