import axios from 'axios'
import { apiClient } from './apiClient'
import type {
  BaristaFailureReason,
  BaristaRedemptionResponse,
  DeviceAuthenticationResponse,
  TodayRedemption,
} from '../types'

export async function authenticateDevice(
  cafeId: number,
  pin: string,
): Promise<DeviceAuthenticationResponse> {
  const response = await apiClient.post<DeviceAuthenticationResponse>(
    '/api/barista/device/authenticate',
    { cafeId, pin },
  )
  return response.data
}

export async function validateQrToken(
  qrToken: string,
): Promise<BaristaRedemptionResponse> {
  const response = await apiClient.post<BaristaRedemptionResponse>(
    '/api/barista/redemptions/validate',
    { qrToken },
  )
  return response.data
}

export async function validateBackupCode(
  backupCode: string,
): Promise<BaristaRedemptionResponse> {
  const response = await apiClient.post<BaristaRedemptionResponse>(
    '/api/barista/redemptions/validate',
    { backupCode },
  )
  return response.data
}

export async function fetchTodayRedemptions(): Promise<TodayRedemption[]> {
  const response = await apiClient.get<TodayRedemption[]>(
    '/api/barista/redemptions/today',
  )
  return response.data
}

export function getFailureReason(error: unknown): BaristaFailureReason {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data
    if (isRecord(data) && isFailureReason(data.reason)) return data.reason
  }
  return 'INVALID_CODE'
}

export function isNetworkError(error: unknown): boolean {
  return axios.isAxiosError(error) && (
    !error.response || error.response.status >= 500
  )
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null
}

function isFailureReason(value: unknown): value is BaristaFailureReason {
  return [
    'INVALID_CODE',
    'CODE_EXPIRED',
    'CODE_ALREADY_USED',
    'WRONG_CAFE',
    'MEMBERSHIP_INACTIVE',
    'INSUFFICIENT_CREDITS',
    'DEVICE_UNAUTHORIZED',
  ].includes(String(value))
}
