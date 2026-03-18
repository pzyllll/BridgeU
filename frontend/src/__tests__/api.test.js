import { describe, it, expect, vi, beforeEach } from 'vitest';

// ====== 全局 mocks：axios 与语言首选项 ======
vi.mock('axios', () => {
  const client = {
    get: vi.fn(),
    post: vi.fn(),
    delete: vi.fn(),
    put: vi.fn(),
    interceptors: {
      request: { use: vi.fn() },
      response: { use: vi.fn() },
    },
  };
  const create = vi.fn(() => client);
  return { default: { create, __client: client } };
});

vi.mock('../utils/language', () => ({
  getLanguagePreference: vi.fn(() => 'en'),
}));

// 需要在 mocks 之后再导入被测函数
import {
  fetchPosts,
  fetchPostDetail,
  createPost,
  searchAll,
  addComment,
  getCommentSummary,
  deleteComment,
  toggleLike,
  login,
  registerWithVerification,
  sendVerificationCode,
  verifyCode,
  registerWithPhone,
  sendPasswordResetCode,
  resetPassword,
  resetPasswordWithPhone,
  getFollowers,
  getUserMutualFollows,
} from '../api';

// 拿到与 api.js 中同一个 axios client 实例
// 注意：在 ESM 中 `import axios from 'axios'` 直接得到的是 default 导出
import axios from 'axios';
const createdClient = axios.__client;

describe('BridgeU Frontend API Tests (Vitest)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('UTC-14: fetchPosts - basic query with explicit lang=en', async () => {
    const inputParams = {
      q: 'scholarship',
      page: 0,
      size: 10,
      lang: 'en',
    };

    const expectedOutput = {
      success: true,
      data: [
        {
          id: 'p1',
          title: 'Scholarship for international students',
        },
      ],
    };

    createdClient.get.mockResolvedValueOnce({ data: expectedOutput });

    console.log('UTC-14 Input:', JSON.stringify(inputParams, null, 2));
    const result = await fetchPosts(inputParams);
    console.log('UTC-14 Output:', JSON.stringify(result, null, 2));

    expect(createdClient.get).toHaveBeenCalledWith('/api/posts', {
      params: inputParams,
      timeout: 60000,
    });

    expect(result).toEqual(expectedOutput);
  });

  it('UTC-14-2: fetchPosts - when lang omitted uses language preference', async () => {
    const inputParams = {
      q: 'visa',
      page: 1,
      size: 5,
      // 不传 lang，应该自动用 getLanguagePreference()
    };

    const expectedOutput = {
      success: true,
      data: [],
    };

    createdClient.get.mockResolvedValueOnce({ data: expectedOutput });

    console.log('UTC-14-2 Input:', JSON.stringify(inputParams, null, 2));
    const result = await fetchPosts(inputParams);
    console.log('UTC-14-2 Output:', JSON.stringify(result, null, 2));

    expect(createdClient.get).toHaveBeenCalledWith('/api/posts', {
      params: { ...inputParams, lang: 'en' },
      timeout: 60000,
    });
    expect(result).toEqual(expectedOutput);
  });

  it('UTC-15: fetchPostDetail - with explicit lang=zh', async () => {
    const postId = 'post-123';
    const lang = 'zh';

    const expectedOutput = {
      success: true,
      post: {
        id: postId,
        title: 'test post detail',
      },
    };

    createdClient.get.mockResolvedValueOnce({ data: expectedOutput });

    const inputJson = { postId, lang };
    console.log('UTC-15 Input:', JSON.stringify(inputJson, null, 2));

    const result = await fetchPostDetail(postId, lang);

    console.log('UTC-15 Output:', JSON.stringify(result, null, 2));

    expect(createdClient.get).toHaveBeenCalledWith(`/api/posts/${postId}`, {
      params: { lang },
    });
    expect(result).toEqual(expectedOutput);
  });

  it('UTC-16: createPost - success (with token header)', async () => {
    const token = 'fake-token';
    const payload = {
      title: 'Hello',
      body: 'First post',
      lang: 'en',
    };

    const expectedOutput = {
      success: true,
      post: { id: 'p100', title: 'Hello' },
    };

    createdClient.post.mockResolvedValueOnce({ data: expectedOutput });

    const result = await createPost(payload, token);

    expect(createdClient.post).toHaveBeenCalledWith('/api/posts', payload, {
      headers: { Authorization: `Bearer ${token}` },
    });
    expect(result).toEqual(expectedOutput);
  });

  it('UTC-17: addComment - success (lang omitted uses preference + token header)', async () => {
    const postId = 'p100';
    const token = 'fake-token';
    const content = 'Nice post';

    const expectedOutput = {
      success: true,
      comment: { id: 'c1', content: 'Nice post' },
    };

    createdClient.post.mockResolvedValueOnce({ data: expectedOutput });

    const result = await addComment(postId, content, undefined, token);

    expect(createdClient.post).toHaveBeenCalledWith(
      `/api/posts/${postId}/comments`,
      { content },
      {
        params: { lang: 'en' },
        headers: { Authorization: `Bearer ${token}` },
      },
    );
    expect(result).toEqual(expectedOutput);
  });

  it('UTC-18: getCommentSummary - success (explicit lang=zh)', async () => {
    const postId = 'p100';
    const lang = 'zh';

    const expectedOutput = {
      success: true,
      summary: { total: 2 },
    };

    createdClient.get.mockResolvedValueOnce({ data: expectedOutput });

    const result = await getCommentSummary(postId, lang);

    expect(createdClient.get).toHaveBeenCalledWith(`/api/posts/${postId}/comments/summary`, {
      params: { lang },
    });
    expect(result).toEqual(expectedOutput);
  });

  it('UTC-19: deleteComment - success (with token header)', async () => {
    const postId = 'p100';
    const commentId = 'c1';
    const token = 'fake-token';

    const expectedOutput = { success: true };

    createdClient.delete.mockResolvedValueOnce({ data: expectedOutput });

    const result = await deleteComment(postId, commentId, token);

    expect(createdClient.delete).toHaveBeenCalledWith(`/api/posts/${postId}/comments/${commentId}`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    expect(result).toEqual(expectedOutput);
  });

  it('UTC-20: toggleLike - success (with token header)', async () => {
    const postId = 'p100';
    const token = 'fake-token';

    const expectedOutput = { success: true, liked: true, likeCount: 1 };

    createdClient.post.mockResolvedValueOnce({ data: expectedOutput });

    const result = await toggleLike(postId, token);

    expect(createdClient.post).toHaveBeenCalledWith(
      `/api/posts/${postId}/like`,
      {},
      { headers: { Authorization: `Bearer ${token}` } },
    );
    expect(result).toEqual(expectedOutput);
  });

  it('UTC-21: searchAll - success (passes through params)', async () => {
    const params = { query: 'visa', lang: 'en' };
    const expectedOutput = { success: true, communities: [], posts: [] };

    createdClient.get.mockResolvedValueOnce({ data: expectedOutput });

    const result = await searchAll(params);

    expect(createdClient.get).toHaveBeenCalledWith('/api/search', { params });
    expect(result).toEqual(expectedOutput);
  });

  it('UTC-22: registerWithVerification - success case', async () => {
    const payload = {
      username: 'alice',
      identifier: 'alice@example.com',
      password: 'P@ssw0rd!',
      code: '123456',
      type: 'email',
      displayName: 'alice',
      preferredLanguage: 'en',
    };

    const expectedOutput = {
      success: true,
      token: 'fake-jwt-token',
      user: {
        id: 'u-1',
        username: 'alice',
      },
    };

    createdClient.post.mockResolvedValueOnce({ data: expectedOutput });

    console.log('UTC-22 Input:', JSON.stringify(payload, null, 2));
    const result = await registerWithVerification(payload);
    console.log('UTC-22 Output:', JSON.stringify(result, null, 2));

    expect(createdClient.post).toHaveBeenCalledWith('/api/auth/register', payload);
    expect(result).toEqual(expectedOutput);
  });

  it('UTC-23: login - success case', async () => {
    const payload = { username: 'alice', password: 'P@ssw0rd!' };

    const expectedOutput = {
      token: 'jwt-token',
      expiresIn: 3600,
      user: { id: 'u-1', username: 'alice' },
    };

    createdClient.post.mockResolvedValueOnce({ data: expectedOutput });

    const result = await login(payload.username, payload.password);

    expect(createdClient.post).toHaveBeenCalledWith('/api/auth/login', payload);
    expect(result).toEqual(expectedOutput);
  });

  it('UTC-22-2: registerWithVerification - server error', async () => {
    const payload = {
      username: 'bob',
      identifier: 'bob@example.com',
      password: 'Password123',
      code: '654321',
      type: 'email',
      displayName: 'bob',
      preferredLanguage: 'en',
    };

    const errorResponse = {
      success: false,
      message: 'Username already taken',
    };

    createdClient.post.mockResolvedValueOnce({ data: errorResponse });

    console.log('UTC-22-2 Input:', JSON.stringify(payload, null, 2));
    const result = await registerWithVerification(payload);
    console.log('UTC-22-2 Output:', JSON.stringify(result, null, 2));

    expect(createdClient.post).toHaveBeenCalledWith('/api/auth/register', payload);
    expect(result).toEqual(errorResponse);
  });

  it('UTC-24-1: Auth - sendVerificationCode (email)', async () => {
    const input = { identifier: 'alice@example.com', type: 'email' };
    const expectedOutput = { success: true };

    createdClient.post.mockResolvedValueOnce({ data: expectedOutput });

    console.log('Auth-sendCode Input:', JSON.stringify(input, null, 2));
    const result = await sendVerificationCode(input.identifier, input.type);
    console.log('Auth-sendCode Output:', JSON.stringify(result, null, 2));

    expect(createdClient.post).toHaveBeenCalledWith(
      '/api/auth/send-verification-code',
      input,
      { timeout: 60000 },
    );
    expect(result).toEqual(expectedOutput);
  });

  it('UTC-24-2: Auth - verifyCode (REGISTER)', async () => {
    const input = {
      identifier: 'alice@example.com',
      code: '123456',
      type: 'email',
      purpose: 'REGISTER',
    };
    const expectedOutput = { success: true, verified: true };

    createdClient.post.mockResolvedValueOnce({ data: expectedOutput });

    console.log('Auth-verifyCode Input:', JSON.stringify(input, null, 2));
    const result = await verifyCode(
      input.identifier,
      input.code,
      input.type,
      input.purpose,
    );
    console.log('Auth-verifyCode Output:', JSON.stringify(result, null, 2));

    expect(createdClient.post).toHaveBeenCalledWith(
      '/api/auth/verify-code',
      input,
    );
    expect(result).toEqual(expectedOutput);
  });

  it('UTC-24-3: Auth - registerWithPhone (success)', async () => {
    const payload = {
      phone: '+66912345678',
      username: 'phoneUser',
      password: 'Password123',
    };
    const expectedOutput = { success: true, userId: 'u-phone' };

    createdClient.post.mockResolvedValueOnce({ data: expectedOutput });

    console.log('Auth-registerPhone Input:', JSON.stringify(payload, null, 2));
    const result = await registerWithPhone(payload);
    console.log('Auth-registerPhone Output:', JSON.stringify(result, null, 2));

    expect(createdClient.post).toHaveBeenCalledWith(
      '/api/auth/register/phone',
      payload,
    );
    expect(result).toEqual(expectedOutput);
  });

  it('UTC-24-4: Auth - sendPasswordResetCode (email)', async () => {
    const input = { identifier: 'alice@example.com', type: 'email' };
    const expectedOutput = { success: true };

    createdClient.post.mockResolvedValueOnce({ data: expectedOutput });

    console.log('Auth-sendPwdCode Input:', JSON.stringify(input, null, 2));
    const result = await sendPasswordResetCode(
      input.identifier,
      input.type,
    );
    console.log('Auth-sendPwdCode Output:', JSON.stringify(result, null, 2));

    expect(createdClient.post).toHaveBeenCalledWith(
      '/api/auth/forgot-password/send-code',
      input,
      { timeout: 60000 },
    );
    expect(result).toEqual(expectedOutput);
  });

  it('UTC-24-5: Auth - resetPassword (email)', async () => {
    const input = {
      identifier: 'alice@example.com',
      code: '123456',
      newPassword: 'Password123',
      type: 'email',
    };
    const expectedOutput = { success: true };

    createdClient.post.mockResolvedValueOnce({ data: expectedOutput });

    console.log('Auth-resetPassword Input:', JSON.stringify(input, null, 2));
    const result = await resetPassword(
      input.identifier,
      input.code,
      input.newPassword,
      input.type,
    );
    console.log('Auth-resetPassword Output:', JSON.stringify(result, null, 2));

    expect(createdClient.post).toHaveBeenCalledWith(
      '/api/auth/forgot-password/reset',
      input,
    );
    expect(result).toEqual(expectedOutput);
  });

  it('UTC-24-6: Auth - resetPasswordWithPhone', async () => {
    const input = {
      phone: '+66912345678',
      newPassword: 'Password123',
    };
    const expectedOutput = { success: true };

    createdClient.post.mockResolvedValueOnce({ data: expectedOutput });

    console.log('Auth-resetPasswordPhone Input:', JSON.stringify(input, null, 2));
    const result = await resetPasswordWithPhone(
      input.phone,
      input.newPassword,
    );
    console.log('Auth-resetPasswordPhone Output:', JSON.stringify(result, null, 2));

    expect(createdClient.post).toHaveBeenCalledWith(
      '/api/auth/forgot-password/reset/phone',
      input,
    );
    expect(result).toEqual(expectedOutput);
  });

  // ====== Feature 123: 关注 / 互相关注相关 API，可测空列表与正常列表 ======

  it('UTC-25-1: getFollowers - returns empty list (empty state)', async () => {
    const userId = 'user123';
    const token = 'fake-token';
    const expectedOutput = {
      success: true,
      data: [],
      count: 0,
    };

    createdClient.get.mockResolvedValueOnce({ data: expectedOutput });

    const inputJson = { userId, token };
    console.log('getFollowers-empty Input:', JSON.stringify(inputJson, null, 2));
    const result = await getFollowers(userId, token);
    console.log('getFollowers-empty Output:', JSON.stringify(result, null, 2));

    expect(createdClient.get).toHaveBeenCalledWith(
      `/api/users/${userId}/followers`,
      { headers: { Authorization: `Bearer ${token}` } },
    );
    expect(result).toEqual(expectedOutput);
  });

  it('UTC-25-2: getFollowers - returns follower list', async () => {
    const userId = 'user123';
    const token = 'fake-token';
    const expectedOutput = {
      success: true,
      data: [
        { id: 'u1', username: 'alice', displayName: 'Alice', avatar: null, isFollowing: true },
        { id: 'u2', username: 'bob', displayName: 'Bob', avatar: null, isFollowing: false },
      ],
      count: 2,
    };

    createdClient.get.mockResolvedValueOnce({ data: expectedOutput });

    const inputJson = { userId, token };
    console.log('getFollowers-list Input:', JSON.stringify(inputJson, null, 2));
    const result = await getFollowers(userId, token);
    console.log('getFollowers-list Output:', JSON.stringify(result, null, 2));

    expect(createdClient.get).toHaveBeenCalledWith(
      `/api/users/${userId}/followers`,
      { headers: { Authorization: `Bearer ${token}` } },
    );
    expect(result).toEqual(expectedOutput);
  });

  it('UTC-25-3: getUserMutualFollows - returns empty list', async () => {
    const userId = 'user123';
    const token = 'fake-token';
    const expectedOutput = {
      success: true,
      data: [],
      count: 0,
    };

    createdClient.get.mockResolvedValueOnce({ data: expectedOutput });

    const inputJson = { userId, token };
    console.log('getUserMutualFollows-empty Input:', JSON.stringify(inputJson, null, 2));
    const result = await getUserMutualFollows(userId, token);
    console.log('getUserMutualFollows-empty Output:', JSON.stringify(result, null, 2));

    expect(createdClient.get).toHaveBeenCalledWith(
      `/api/users/${userId}/mutual-follows`,
      { headers: { Authorization: `Bearer ${token}` } },
    );
    expect(result).toEqual(expectedOutput);
  });

  it('UTC-25-4: getUserMutualFollows - returns mutual follow list', async () => {
    const userId = 'user123';
    const token = 'fake-token';
    const expectedOutput = {
      success: true,
      data: [
        { id: 'u10', username: 'carol', displayName: 'Carol', avatar: null, isFollowing: true },
      ],
      count: 1,
    };

    createdClient.get.mockResolvedValueOnce({ data: expectedOutput });

    const inputJson = { userId, token };
    console.log('getUserMutualFollows-list Input:', JSON.stringify(inputJson, null, 2));
    const result = await getUserMutualFollows(userId, token);
    console.log('getUserMutualFollows-list Output:', JSON.stringify(result, null, 2));

    expect(createdClient.get).toHaveBeenCalledWith(
      `/api/users/${userId}/mutual-follows`,
      { headers: { Authorization: `Bearer ${token}` } },
    );
    expect(result).toEqual(expectedOutput);
  });
});