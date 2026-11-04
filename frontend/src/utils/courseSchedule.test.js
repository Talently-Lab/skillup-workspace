import assert from 'node:assert/strict'
import test from 'node:test'
import {
  calculateScheduleSummary,
  formatMinutes,
  getRecordedContentMinutes,
  getSessionMinutes,
  parseScheduleDays,
} from './courseSchedule.js'

test('parses unique weekday names with accents and conjunctions', () => {
  assert.deepEqual(parseScheduleDays('Lunes y miércoles, Lunes'), ['lunes', 'miercoles'])
})

test('calculates session duration only when the end time follows the start time', () => {
  assert.equal(getSessionMinutes('09:30', '11:00'), 90)
  assert.equal(getSessionMinutes('11:00', '09:30'), 0)
})

test('calculates live workload from weekly sessions and estimated weeks', () => {
  assert.deepEqual(
    calculateScheduleSummary({
      modality: 'EN VIVO',
      days: ['lunes', 'miercoles'],
      sessionStartTime: '09:00',
      sessionEndTime: '10:30',
      durationWeeks: 4,
    }),
    {
      sessionMinutes: 90,
      sessionsPerWeek: 2,
      weeklyMinutes: 180,
      liveMinutes: 720,
      totalDurationMinutes: 720,
    },
  )
})

test('includes recorded content in hybrid and recorded workloads', () => {
  const schedule = {
    days: ['martes'],
    sessionStartTime: '10:00',
    sessionEndTime: '11:00',
    durationWeeks: 3,
    recordedContentMinutes: 120,
  }

  assert.equal(calculateScheduleSummary({ modality: 'HÍBRIDO', ...schedule }).totalDurationMinutes, 300)
  assert.equal(calculateScheduleSummary({ modality: 'GRABADO', ...schedule }).totalDurationMinutes, 120)
})

test('formats exact workload without rounding away minutes', () => {
  assert.equal(formatMinutes(95), '1 hora 35 min')
})

test('recovers recorded content when opening courses saved before the schedule fields existed', () => {
  assert.equal(
    getRecordedContentMinutes({
      modality: { type: 'HÍBRIDO' },
      durationHours: 13,
      durationWeeks: 4,
      schedule: { days: 'Lunes', startTime: '17:30', endTime: '19:00' },
    }),
    420,
  )
  assert.equal(
    getRecordedContentMinutes({
      modality: { type: 'GRABADO' },
      durationHours: 5,
      schedule: {},
    }),
    300,
  )
})

test('does not treat a null schedule field as a saved zero-minute estimate', () => {
  assert.equal(
    getRecordedContentMinutes({
      modality: { type: 'GRABADO' },
      durationHours: 5,
      schedule: { recordedContentMinutes: null },
    }),
    300,
  )
})
