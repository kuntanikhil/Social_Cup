Social Cup ☕

Social Cup is a coffee membership and discovery platform designed for Dallas. Members subscribe monthly, receive drink credits, discover partner cafes, redeem credits for real drinks, and rate individual drinks to build a personal drink diary.

The platform is being built as a multi-surface application consisting of:

A React Native mobile app for members and visitors

A React admin panel for the Social Cup team

A lightweight browser-based scan page for partner cafe staff

A Java Spring Boot REST API

A PostgreSQL database

Project Status: Prototype / Active Development

Product Overview

Social Cup connects coffee lovers with independent partner cafes.

Members pay $24.99 per month and receive 30 drink credits per billing cycle. Credits can be redeemed for physical drinks at participating cafes.

Visitors can browse cafes, search and filter the network, rate drinks, and maintain a drink diary without subscribing.

Cafe staff do not need an account or a dedicated app. They use a private browser-based scan page to validate a member's redemption code.

Core User Roles

Visitor

A registered user who has not subscribed.

Visitors can:

Browse partner cafes

Search and filter cafes

View cafe menus

Rate individual drinks

Maintain a personal drink diary

Member

A paying subscriber.

Members receive everything available to Visitors, plus:

30 drink credits per billing cycle

In-app subscription through Stripe

Drink redemption at partner cafes

QR and six-digit backup redemption codes

Barista

Cafe staff validating a redemption.

Baristas:

Use a browser-based scan page

Enter the cafe PIN once to trust the device

Scan a member's QR code or enter the six-digit backup code

Receive a simple green or red validation result

View the cafe's redemptions for the current day

Platform Administrator

The Social Cup operations team.

Administrators manage:

Partner cafes

Cafe payout rates

Drink menus

Credit pricing

Featured cafes

Signature drinks

Members

Redemption history

Redemption voids

Cafe payouts and statements

Technology Stack

Mobile

React Native

Expo

Backend

Java 25

Spring Boot

Spring Web

Spring Data JPA

Hibernate

Spring Security

Maven

Flyway

Database

PostgreSQL

Supabase PostgreSQL for the prototype

Web Applications

React Admin Panel

Lightweight React/Web Barista Scanner

Payments

Stripe

Stripe Test Mode during prototype development

Apple Pay

Google Pay

Credit / Debit Card

Storage

Supabase Storage for cafe, drink, and profile images

Monitoring

Sentry

High-Level Architecture

                     SOCIAL CUP
                          |
        +-----------------+-----------------+
        |                 |                 |
        v                 v                 v
 React Native App    React Admin      Barista Web
        |                 |                 |
        +-----------------+-----------------+
                          |
                       HTTPS
                          |
                          v
                Java Spring Boot API
                          |
          +---------------+---------------+
          |               |               |
          v               v               v
     PostgreSQL         Stripe        File Storage
      Supabase                        Supabase

The Spring Boot backend is the central authority for business rules.

The mobile application and web clients never directly modify financial state such as credit balances or completed redemptions.

Core Product Modules

1. Platform Setup

Environment configuration

HTTPS

Database migrations

Monitoring

Scheduled jobs

Transactional email

2. Authentication & Onboarding

Email/password registration

Google Sign-In

Apple Sign-In

Email verification

Password reset

Profile setup

Coffee preferences

Home neighbourhood

Location permission

3. Shop Discovery

Featured cafes

Signature drinks

Preference matching

Distance ordering

Neighbourhood filtering

Cafe search

4. Cafe Details

Photo gallery

Opening hours

Address and directions

Full menu

Retail and credit prices

Drink ratings

Redeem action

5. Ratings & Drink Diary

1–5 star drink ratings

Optional 140-character notes

One rating per user per drink

Editable ratings

Personal drink diary

Cafe score derived from drink ratings

6. Curated Discovery

Ranking order for Phase 1:

Featured cafes

Preference-matched cafes

Remaining cafes ordered by distance

No recommendation scoring or background ranking engine is used in Phase 1.

7. Membership & Credits

$24.99 monthly membership

30 drink credits per billing cycle

No rollover

No credit top-ups

Stripe subscription lifecycle

Credit ledger

Payment failure handling

8. Redemption

One live redemption code per member

Code valid for five minutes

QR code

Six-digit backup code

One cafe + one drink per redemption session

Server-side validation

Transactional credit deduction

Replay protection

Cafe payout snapshot

9. Admin Panel

Dashboard

Cafe management

Menu management

Pricing calculator

Member management

Redemption log

Void handling

CSV export

Payout recording

10. Quality Assurance

Unit testing

Integration testing

PostgreSQL-backed tests

Concurrency testing

Replay and expiry testing

iOS and Android regression

Browser testing

Real-device UAT

Membership Rules

Monthly Price:     $24.99
Monthly Credits:   30
Credit Value:      $1
Rollover:          No
Credit Top-Ups:    No

Credits are granted only after Stripe confirms a successful payment.

Credits are deducted only after a barista successfully validates a redemption.

Generating or displaying a redemption code does not deduct credits.

Redemption Flow

Member opens cafe
        |
        v
Selects Redeem
        |
        v
Chooses drink
        |
        v
Reviews credit cost
        |
        v
Confirms at counter
        |
        v
5-minute redemption session
        |
        +------ QR Code
        |
        +------ 6-digit backup code
        |
        v
Barista validates
        |
        v
Spring Boot performs locked transaction
        |
        +------ Validate membership
        +------ Validate cafe
        +------ Validate drink
        +------ Validate expiry
        +------ Validate credit balance
        |
        v
Deduct credits once
        |
        v
Record cafe payout
        |
        v
GREEN

A single redemption session must never result in more than one successful credit deduction.

Credit Ledger

The project uses a ledger-based approach instead of relying only on a mutable credit balance.

Examples of credit transactions:

CYCLE_GRANT
CYCLE_EXPIRY
REDEMPTION
VOID_REFUND

This provides an auditable history of how a member's balance changed.

Cafe Payouts

Each cafe has an administrator-defined payout rate per credit.

At the moment a redemption succeeds, the backend stores a snapshot of that rate.

Example:

Drink Cost:             5 credits
Credit Value:           $1.00
Cafe Payout Rate:       $0.65 / credit

Member Value:           $5.00
Cafe Payout:            $3.25
Social Cup Margin:      $1.75

Historical redemption values are never recalculated using a cafe's future payout rate.

Repository Structure

social-cup/
|
+-- backend/
|   +-- Spring Boot API
|
+-- mobile/
|   +-- React Native + Expo
|
+-- admin-web/
|   +-- React Admin Panel
|
+-- barista-web/
|   +-- Cafe Scanner
|
+-- docs/
|   +-- architecture/
|   +-- api/
|   +-- database/
|   +-- testing/
|
+-- README.md

Backend Package Structure

The Spring Boot backend is organized by business capability.

com.socialcup
|
+-- auth
+-- users
+-- profiles
+-- cafes
+-- drinks
+-- discovery
+-- ratings
+-- memberships
+-- billing
+-- credits
+-- redemptions
+-- barista
+-- payouts
+-- admin
+-- storage
+-- email
+-- jobs
+-- common

Social Cup is implemented as a modular monolith for the prototype.

Local Backend Setup

Requirements

Java 25

Maven / Maven Wrapper

PostgreSQL-compatible database

Git

Verify Java:

java -version
javac -version

Run the backend

Windows:

mvnw.cmd spring-boot:run

macOS/Linux:

./mvnw spring-boot:run

The backend runs locally at:

http://localhost:8080

Health endpoint:

GET /api/health

Expected response:

Social Cup API is running

Environment Variables

Database and external-service credentials must not be committed to Git.

Example variables:

DB_URL
DB_USERNAME
DB_PASSWORD

JWT_SECRET

STRIPE_SECRET_KEY
STRIPE_WEBHOOK_SECRET

EMAIL_API_KEY

SUPABASE_URL
SUPABASE_STORAGE_KEY

Use local environment configuration or secure cloud environment variables.

Database Migrations

Flyway manages all PostgreSQL schema changes.

Migration files are stored under:

backend/src/main/resources/db/migration/

Example:

V1__create_neighbourhoods.sql
V2__create_users.sql
V3__create_auth_identities.sql

Once a migration has been executed, it should not be edited. New schema changes should be added through a new migration.

API Design

The backend exposes REST APIs for:

/api/auth/**
/api/profile/**
/api/discover
/api/cafes/**
/api/drinks/**
/api/membership/**
/api/redemption-sessions/**
/api/barista/**
/api/admin/**
/api/webhooks/stripe

Spring Boot owns all business validation and financial state changes.

Prototype Scope

The current prototype focuses on proving the complete Social Cup flow:

Account
  ->
Discovery
  ->
Cafe
  ->
Membership
  ->
Credits
  ->
Redemption
  ->
Barista Validation
  ->
Rating
  ->
Admin Visibility

Production hardening and nonessential Phase 2 capabilities will be handled after the core prototype is working.

Out of Scope for Phase 1

Cafe self-service portal

Automated bank payouts

Push notifications

Advanced analytics

Community-based recommendation ranking

Credit top-ups

Order-ahead

Offline cafe scanning

Social connections

Activity feed

Saved cafes

Meetup planning

Development Principles

The backend is authoritative.

Financial history is never silently deleted.

Credits can never become negative.

A redemption can succeed at most once.

Stripe events must be processed idempotently.

Historical cafe payout rates must remain immutable.

Frontend validation improves UX; backend validation protects the system.

PostgreSQL constraints reinforce important business rules.

Secrets never belong in source control.

Build the working end-to-end flow before overengineering.

Project Status

🚧 Active prototype development

Current focus:

Spring Boot backend foundation

PostgreSQL integration

Flyway database migrations

Core Social Cup domain model

License

This project is currently intended for private prototype and development use.
