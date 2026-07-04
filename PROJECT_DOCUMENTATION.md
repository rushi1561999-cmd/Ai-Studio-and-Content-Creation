# AI Studio Project Documentation

## 1. Project Work Process

### Project Objectives

AI Studio is a comprehensive AI-powered content generation platform that enables users to:
- Generate AI content (text, images, videos) using multiple AI providers
- Manage workspaces and collaborate with team members
- Store and organize generated content and assets
- Share prompts and content in a community marketplace
- Manage credits and subscriptions for AI usage
- Process payments via multiple payment gateways (Stripe, Razorpay)
- Receive notifications and track activity

### System Architecture/Design

#### Technology Stack
- **Backend Framework**: Spring Boot 4.0.6
- **Language**: Java 21
- **Database**: MySQL with JPA (Hibernate)
- **Database Migrations**: Flyway
- **Authentication**: JWT (JSON Web Tokens) with Spring Security
- **Payment Gateways**: Stripe, Razorpay
- **AI Providers**: Google Gemini, OpenAI, Replicate
- **Message Queue**: RabbitMQ (optional, for async job processing)
- **Email Service**: Spring Boot Mail with Thymeleaf templates
- **WebSocket**: For real-time notifications
- **QR Code Generation**: ZXing library

#### Architecture Pattern
The application follows a layered architecture:
- **Controller Layer**: REST API endpoints (13 controllers)
- **Service Layer**: Business logic (25+ services)
- **Repository Layer**: Data access (31 repositories)
- **Entity Layer**: Database models (32 entities)
- **DTO Layer**: Data transfer objects (29 DTOs)
- **Security Layer**: JWT authentication and authorization

#### Key Design Decisions
1. **Credit-Based System**: Users purchase credits which are consumed based on content type (TEXT=1, IMAGE=3, VIDEO=10, MIXED=5)
2. **Workspace Model**: Multi-tenant architecture with workspaces as the primary organizational unit
3. **Async Processing**: AI generation jobs are processed asynchronously (either via RabbitMQ or in-app async)
4. **Payment Flexibility**: Support for multiple payment providers (Stripe, Razorpay, manual)
5. **Marketplace**: Community-driven prompt sharing with likes, comments, and saves
6. **Audit Logging**: Comprehensive audit trail for all platform activities

### Development Progress

#### Completed Features
- ✅ User authentication and authorization with JWT
- ✅ User registration, login, password reset
- ✅ Workspace creation and management
- ✅ Multi-role workspace membership (OWNER, ADMIN, EDITOR, VIEWER)
- ✅ AI content generation (text, image, video)
- ✅ Credit-based billing system with wallet management
- ✅ Payment integration with Stripe and Razorpay
- ✅ UPI QR code generation for Razorpay payments
- ✅ Marketplace for prompt sharing
- ✅ Asset and folder management
- ✅ Notification system with real-time updates
- ✅ Admin dashboard with user management
- ✅ Audit logging system
- ✅ Email notifications
- ✅ Database migrations with Flyway

#### Database Schema
- ✅ 32 entity models designed and implemented
- ✅ Proper relationships and constraints
- ✅ Flyway migration for schema updates
- ✅ Normalized database structure

#### API Endpoints
- ✅ 13 REST controllers
- ✅ 50+ API endpoints
- ✅ Comprehensive request/response DTOs
- ✅ Proper error handling and validation

### Challenges Faced and Solutions Implemented

#### Challenge 1: Async Job Processing
**Problem**: AI generation tasks can take time, blocking the main thread
**Solution**: Implemented dual-mode processing:
- RabbitMQ for production environments
- In-app async processing for local development
- Transaction synchronization to ensure job creation before processing

#### Challenge 2: Payment Gateway Integration
**Problem**: Need to support multiple payment providers for global users
**Solution**: 
- Abstracted payment service layer
- Implemented Stripe for international payments
- Implemented Razorpay with UPI support for Indian users
- Webhook handlers for payment confirmation
- QR code generation for UPI payments

#### Challenge 3: Credit System Consistency
**Problem**: Ensuring credits are deducted correctly even if AI generation fails
**Solution**:
- Transactional job creation
- Credit deduction before job processing
- Rollback mechanism for failed jobs
- Comprehensive transaction history

#### Challenge 4: Database Schema Evolution
**Problem**: Need to update schema without breaking existing data
**Solution**:
- Implemented Flyway for version-controlled migrations
- Application-level migration for critical column renames
- Backward compatibility considerations

#### Challenge 5: Real-time Notifications
**Problem**: Users need instant updates on job status and platform events
**Solution**:
- WebSocket configuration for real-time communication
- Notification entity with read/unread status
- Email notifications for important events
- Unread count tracking

---

## 2. Database Design

### Database Schema Overview

The database follows a normalized design with 32 tables organized into logical groups:

#### User Management
- **users** - User accounts with authentication
- **roles** - Platform-wide role definitions
- **password_reset_tokens** - Password recovery tokens

#### Workspace Management
- **workspaces** - Organizational units
- **workspace_members** - Workspace membership with roles
- **folders** - Hierarchical folder structure for assets

#### AI & Content Generation
- **ai_models** - Available AI models and pricing
- **ai_jobs** - AI generation job tracking
- **generation_jobs** - Content generation requests
- **generated_contents** - Stored AI-generated content
- **prompt_history** - Historical prompt usage

#### Billing & Payments
- **wallets** - Credit wallets per workspace
- **credit_transactions** - Credit transaction history
- **subscription_plans** - Available subscription tiers
- **subscriptions** - Active subscriptions
- **payments** - Generic payment records
- **stripe_payments** - Stripe-specific payment data
- **razorpay_payments** - Razorpay-specific payment data

#### Content Management
- **assets** - User-uploaded assets
- **asset_versions** - Version history for assets
- **prompts** - User-created prompts
- **prompt_templates** - Reusable prompt templates
- **prompt_scores** - Quality scoring for prompts
- **categories** - Categorization for prompts
- **saved_prompts** - Bookmarked marketplace prompts

#### Marketplace & Social
- **marketplace_posts** - Community-shared prompts
- **comments** - Comments on marketplace posts
- **likes** - Likes on marketplace posts
- **followers** - User following relationships

#### System
- **notifications** - User notifications
- **audit_logs** - System audit trail

### Normalization Analysis

#### First Normal Form (1NF) ✅
- All tables have primary keys
- No repeating groups
- Atomic values in all columns
- Proper use of TEXT columns for large content

#### Second Normal Form (2NF) ✅
- All non-key attributes are fully dependent on the primary key
- No partial dependencies
- Example: `workspace_members` depends on both `workspace_id` and `user_id`

#### Third Normal Form (3NF) ✅
- No transitive dependencies
- Example: User email is stored in `users` table, not repeated in `workspace_members`
- Payment amounts stored in `payments`, not duplicated in provider-specific tables

### Table Relationships and Constraints

#### Key Relationships

**User-Workspace Relationship**
```
users (1) ←→ (N) workspace_members ←→ (1) workspaces
```
- Many-to-many relationship through `workspace_members` junction table
- Unique constraint on (workspace_id, user_id) to prevent duplicate memberships
- WorkspaceRole enum: OWNER, ADMIN, EDITOR, VIEWER

**Workspace-Assets Relationship**
```
workspaces (1) ←→ (N) assets
workspaces (1) ←→ (N) folders
folders (1) ←→ (N) assets (optional)
```
- Assets belong to workspaces
- Optional folder assignment for hierarchical organization
- Self-referencing folder structure via `parent_id`

**AI Generation Flow**
```
users → generation_jobs → generated_contents
generation_jobs → ai_models (via model_key)
```
- Jobs track the generation process
- Results stored in generated_contents
- Model information captured for billing

**Billing Flow**
```
workspaces (1) ←→ (1) wallets
wallets (1) ←→ (N) credit_transactions
workspaces (1) ←→ (N) payments
payments → stripe_payments / razorpay_payments
```
- One wallet per workspace
- All credit changes logged
- Payment provider-specific data in separate tables

**Marketplace Social Features**
```
users (1) ←→ (N) marketplace_posts
marketplace_posts (1) ←→ (N) comments
marketplace_posts (1) ←→ (N) likes
users (1) ←→ (N) saved_prompts ←→ (1) marketplace_posts
users (1) ←→ (N) followers ←→ (1) users
```
- Users can create posts, comment, like, and save
- Following relationship between users
- Unique constraints prevent duplicate likes/saves

#### Important Constraints

**Unique Constraints**
- `users.email` - Unique email addresses
- `workspace_members(workspace_id, user_id)` - Prevent duplicate memberships
- `likes(post_id, user_id)` - One like per user per post
- `saved_prompts(user_id, post_id)` - One save per user per post
- `followers(follower_id, following_id)` - Prevent duplicate follows
- `password_reset_tokens.token` - Unique reset tokens

**Foreign Key Constraints**
- All relationships properly enforced with foreign keys
- Cascade deletes configured where appropriate
- Lazy loading for performance optimization

**Data Integrity**
- Enum types for status fields (JobStatus, PaymentStatus, etc.)
- NOT NULL constraints on critical fields
- Default values for status fields
- Timestamps for audit trail (created_at, updated_at)

### Database Migration

**Flyway Configuration**
- Enabled in application.properties
- Migration location: `classpath:db/migration`
- Baseline migration support for existing databases
- Version-controlled schema changes

**Current Migration**
- V2__update_notification_column.sql: Renamed `read` column to `is_read` in notifications table
- Application-level migration in DemoApplication.java for backward compatibility

---

## 3. APIs

### API Overview

The application exposes 50+ RESTful API endpoints organized into 13 controllers. All APIs use JWT authentication except for public endpoints (register, login, password reset, webhooks).

### Authentication APIs (`/api/auth`)

#### POST `/api/auth/register`
**Description**: Register a new user account
**Request Body**:
```json
{
  "email": "user@example.com",
  "password": "SecurePassword123",
  "fullName": "John Doe"
}
```
**Response**: `AuthResponse` with JWT token and user details
**Authentication**: Not required

#### POST `/api/auth/login`
**Description**: Authenticate user and receive JWT token
**Request Body**:
```json
{
  "email": "user@example.com",
  "password": "SecurePassword123"
}
```
**Response**: `AuthResponse` with JWT token
**Authentication**: Not required

#### POST `/api/auth/forgot-password`
**Description**: Request password reset email
**Request Body**:
```json
{
  "email": "user@example.com"
}
```
**Response**: `PasswordResetResponse` with reset token (in dev mode)
**Authentication**: Not required

#### POST `/api/auth/reset-password`
**Description**: Reset password using token
**Request Body**:
```json
{
  "token": "reset-token-here",
  "newPassword": "NewSecurePassword123"
}
```
**Response**: `MessageResponse`
**Authentication**: Not required

#### GET `/api/auth/me`
**Description**: Get current user profile
**Response**: `UserProfileResponse`
**Authentication**: Required

#### PUT `/api/auth/me`
**Description**: Update current user profile
**Request Body**: `UpdateUserRequest`
**Response**: `UpdateUserResponse`
**Authentication**: Required

#### DELETE `/api/auth/me`
**Description**: Delete current user account
**Request Body**: `DeleteAccountRequest` with password confirmation
**Response**: `MessageResponse`
**Authentication**: Required

### AI Generation APIs (`/api/ai`)

#### GET `/api/ai/content-types`
**Description**: Get available content types and their credit costs
**Response**: Array of `ContentTypeInfo`
```json
[
  {
    "type": "TEXT",
    "name": "Text",
    "description": "Articles, copy, explanations",
    "creditCost": 1
  },
  {
    "type": "IMAGE",
    "name": "Image",
    "description": "AI images from your prompt",
    "creditCost": 3
  },
  {
    "type": "VIDEO",
    "name": "Video",
    "description": "Short AI video clips",
    "creditCost": 10
  },
  {
    "type": "MIXED",
    "name": "Rich content",
    "description": "Text + image combined",
    "creditCost": 5
  }
]
```
**Authentication**: Required

#### GET `/api/ai/wallet/{workspaceId}`
**Description**: Get wallet balance for workspace
**Response**: `Wallet` with credit balance
**Authentication**: Required (workspace access)

#### POST `/api/ai/wallet/{workspaceId}/topup`
**Description**: Manually top-up credits (admin use)
**Parameters**: `amount` (1-10000)
**Response**: Updated `Wallet`
**Authentication**: Required (workspace access)

#### POST `/api/ai/generate`
**Description**: Submit AI content generation job
**Request Body**:
```json
{
  "promptText": "Generate a blog post about AI",
  "workspaceId": "workspace-uuid",
  "contentType": "TEXT",
  "modelKey": "gemini-2.0-flash"
}
```
**Response**: `GenerationJob` with job ID and status
**Authentication**: Required (workspace access)
**Credit Cost**: Based on content type

#### POST `/api/ai/generate/simple`
**Description**: Legacy simple generation endpoint (backward compatibility)
**Parameters**: `promptText`, `workspaceId`
**Response**: `GenerationJob`
**Authentication**: Required

#### GET `/api/ai/jobs/{jobId}`
**Description**: Get status of a specific generation job
**Response**: `GenerationJob` with current status and result
**Authentication**: Required (workspace access)

#### GET `/api/ai/jobs/workspace/{workspaceId}`
**Description**: Get all generation jobs for a workspace
**Response**: Array of `GenerationJob` ordered by creation date
**Authentication**: Required (workspace access)

#### GET `/api/ai/contents/workspace/{workspaceId}`
**Description**: Get all generated content for a workspace
**Response**: Array of `GeneratedContent` ordered by creation date
**Authentication**: Required (workspace access)

### Workspace APIs (`/api/workspaces`)

#### POST `/api/workspaces`
**Description**: Create a new workspace
**Request Body**:
```json
{
  "name": "My Workspace"
}
```
**Response**: `WorkspaceResponse` with workspace details
**Authentication**: Required

#### GET `/api/workspaces`
**Description**: Get all workspaces for current user
**Response**: Array of `WorkspaceResponse`
**Authentication**: Required

### Prompt APIs (`/api/prompts`)

#### POST `/api/prompts`
**Description**: Create a new prompt
**Request Body**:
```json
{
  "title": "Blog Post Generator",
  "content": "Write a blog post about {topic}",
  "categoryId": "category-uuid",
  "workspaceId": "workspace-uuid"
}
```
**Response**: `PromptResponse`
**Authentication**: Required

#### GET `/api/prompts/workspace/{workspaceId}`
**Description**: Get all prompts for a workspace
**Response**: Array of `PromptResponse`
**Authentication**: Required

### Marketplace APIs (`/api/marketplace`)

#### GET `/api/marketplace/feed`
**Description**: Get marketplace feed of shared prompts
**Response**: Array of `MarketplacePost`
**Authentication**: Required

#### POST `/api/marketplace/publish`
**Description**: Publish a prompt to marketplace
**Request Body**:
```json
{
  "promptText": "Generate a story about {theme}",
  "category": "Creative Writing",
  "authorName": "John Doe"
}
```
**Response**: `MarketplacePost`
**Authentication**: Required

#### POST `/api/marketplace/{postId}/like`
**Description**: Like a marketplace post
**Response**: Updated `MarketplacePost`
**Authentication**: Required

### Asset Management APIs (`/api/assets`)

#### GET `/api/assets/workspace/{workspaceId}`
**Description**: List all assets in a workspace
**Response**: Array of `Asset`
**Authentication**: Required (workspace access)

#### POST `/api/assets`
**Description**: Register a new asset
**Request Body**:
```json
{
  "workspaceId": "workspace-uuid",
  "folderId": "folder-uuid",
  "name": "document.pdf",
  "mimeType": "application/pdf",
  "sizeBytes": 1024000
}
```
**Response**: `Asset`
**Authentication**: Required

#### GET `/api/assets/{assetId}/versions`
**Description**: Get version history for an asset
**Response**: Array of `AssetVersion`
**Authentication**: Required

#### GET `/api/assets/workspace/{workspaceId}/folders`
**Description**: List all folders in a workspace
**Response**: Array of `Folder`
**Authentication**: Required (workspace access)

#### POST `/api/assets/workspace/{workspaceId}/folders`
**Description**: Create a new folder
**Request Body**:
```json
{
  "name": "Documents",
  "parentId": "parent-folder-uuid"
}
```
**Response**: `Folder`
**Authentication**: Required

### Billing APIs (`/api/billing`)

#### GET `/api/billing/plans`
**Description**: Get available subscription plans
**Response**: Array of `SubscriptionPlan`
**Authentication**: Not required

#### GET `/api/billing/workspace/{workspaceId}/transactions`
**Description**: Get credit transaction history
**Response**: Array of `CreditTransaction` ordered by date
**Authentication**: Required (workspace access)

#### GET `/api/billing/workspace/{workspaceId}/payments`
**Description**: Get payment history
**Response**: Array of `Payment` ordered by date
**Authentication**: Required (workspace access)

#### GET `/api/billing/workspace/{workspaceId}/payment-methods`
**Description**: Get available payment methods (placeholder)
**Response**: Empty array (not implemented)
**Authentication**: Required (workspace access)

#### GET `/api/billing/workspace/{workspaceId}/subscription`
**Description**: Get current subscription
**Response**: `Subscription` or null
**Authentication**: Required (workspace access)

#### POST `/api/billing/workspace/{workspaceId}/subscribe`
**Description**: Subscribe to a plan
**Request Body**:
```json
{
  "planId": "plan-uuid"
}
```
**Response**: `Subscription`
**Authentication**: Required (workspace access)

#### POST `/api/billing/workspace/{workspaceId}/subscription/{subscriptionId}/cancel`
**Description**: Cancel subscription
**Response**: 204 No Content
**Authentication**: Required (workspace access)

### Notification APIs (`/api/notifications`)

#### GET `/api/notifications`
**Description**: Get all notifications for current user
**Response**: Array of `Notification`
**Authentication**: Required

#### GET `/api/notifications/unread-count`
**Description**: Get unread notification count
**Response**: `{"count": 5}`
**Authentication**: Required

#### PATCH `/api/notifications/{id}/read`
**Description**: Mark notification as read
**Response**: Updated `Notification`
**Authentication**: Required

#### PATCH `/api/notifications/read-all`
**Description**: Mark all notifications as read
**Response**: `{"updated": 10}`
**Authentication**: Required

### Stripe Payment APIs (`/api/stripe`)

#### GET `/api/stripe/status`
**Description**: Check if Stripe is configured
**Response**: `{"enabled": true}`
**Authentication**: Not required

#### POST `/api/stripe/checkout`
**Description**: Create Stripe checkout session
**Parameters**: `workspaceId`, `pack` (credit package)
**Response**: `StripeCheckoutResponse` with checkout URL
**Authentication**: Required

#### POST `/api/stripe/webhook`
**Description**: Handle Stripe webhook events
**Headers**: `Stripe-Signature`
**Request Body**: Raw webhook payload
**Response**: "ok"
**Authentication**: Not required (webhook signature verified)

### Razorpay Payment APIs (`/api/razorpay`)

#### GET `/api/razorpay/status`
**Description**: Check if Razorpay is configured
**Response**: `{"enabled": true, "upiEnabled": true}`
**Authentication**: Not required

#### POST `/api/razorpay/order`
**Description**: Create Razorpay order
**Parameters**: `workspaceId`, `pack`
**Response**: `RazorpayCheckoutResponse` with order details
**Authentication**: Required

#### POST `/api/razorpay/upi/qr`
**Description**: Generate UPI QR code for payment
**Parameters**: `workspaceId`, `pack`
**Response**: `UpiQrResponse` with QR code data
**Authentication**: Required

#### POST `/api/razorpay/verify`
**Description**: Verify Razorpay payment
**Parameters**: `orderId`, `paymentId`, `signature`
**Response**: "Payment verified successfully"
**Authentication**: Required

#### POST `/api/razorpay/webhook`
**Description**: Handle Razorpay webhook events
**Headers**: `X-Razorpay-Signature`
**Request Body**: Raw webhook payload
**Response**: "Webhook processed"
**Authentication**: Not required (webhook signature verified)

### Admin APIs (`/api/admin`)

#### GET `/api/admin/stats`
**Description**: Get platform statistics
**Response**: `AdminStatsResponse` with user, workspace, payment counts
**Authentication**: Required (ADMIN role)

#### GET `/api/admin/users`
**Description**: List all users
**Response**: Array of `AdminUserResponse`
**Authentication**: Required (ADMIN role)

#### PATCH `/api/admin/users/{userId}/role`
**Description**: Update user platform role
**Request Body**: `{"role": "ADMIN"}`
**Response**: Updated `AdminUserResponse`
**Authentication**: Required (ADMIN role)

#### DELETE `/api/admin/users/{userId}`
**Description**: Delete a user
**Response**: `MessageResponse`
**Authentication**: Required (ADMIN role)

#### GET `/api/admin/workspaces`
**Description**: List all workspaces
**Response**: Array of `Workspace`
**Authentication**: Required (ADMIN role)

#### GET `/api/admin/marketplace/posts`
**Description**: List all marketplace posts
**Response**: Array of `MarketplacePost`
**Authentication**: Required (ADMIN role)

#### DELETE `/api/admin/marketplace/posts/{postId}`
**Description**: Delete a marketplace post
**Response**: 204 No Content
**Authentication**: Required (ADMIN role)

#### GET `/api/admin/audit-logs`
**Description**: Get audit log entries
**Response**: Array of `AuditLog`
**Authentication**: Required (ADMIN role)

#### GET `/api/admin/payments`
**Description**: List all payments
**Response**: Array of `Payment`
**Authentication**: Required (ADMIN role)

#### GET `/api/admin/ai-models`
**Description**: List all AI models
**Response**: Array of `AiModel`
**Authentication**: Required (ADMIN role)

#### PATCH `/api/admin/ai-models/{modelId}`
**Description**: Toggle AI model active status
**Request Body**: `{"active": true}`
**Response**: Updated `AiModel`
**Authentication**: Required (ADMIN role)

#### POST `/api/admin/users/{userId}/add-credits`
**Description**: Add credits to user's workspace
**Request Body**: `{"amount": 100, "description": "Bonus credits"}`
**Response**: Updated `Wallet`
**Authentication**: Required (ADMIN role)

### AI Job APIs (`/api/jobs`)

#### POST `/api/jobs/generate`
**Description**: Legacy AI generation endpoint
**Request Body**: `{"prompt": "Generate content"}`
**Response**: `AiJob`
**Authentication**: Required
**Note**: This is a legacy endpoint, use `/api/ai/generate` instead

### API Testing and Integration

#### Authentication
All protected endpoints require JWT token in Authorization header:
```
Authorization: Bearer <jwt-token>
```

#### Error Responses
Standard error format:
```json
{
  "message": "Error description",
  "status": 400,
  "timestamp": "2024-01-01T00:00:00"
}
```

#### Common HTTP Status Codes
- **200 OK**: Successful request
- **201 Created**: Resource created successfully
- **204 No Content**: Successful request with no response body
- **400 Bad Request**: Invalid request parameters
- **401 Unauthorized**: Missing or invalid authentication
- **403 Forbidden**: Insufficient permissions
- **404 Not Found**: Resource not found
- **402 Payment Required**: Insufficient credits
- **500 Internal Server Error**: Server error

#### Testing Recommendations
1. **Authentication Flow**: Test register → login → token usage
2. **Workspace Creation**: Create workspace and verify membership
3. **AI Generation**: Test all content types with credit deduction
4. **Payment Flow**: Test Stripe and Razorpay checkout → webhook → credit addition
5. **Marketplace**: Test publish → like → save functionality
6. **Admin Operations**: Test admin-only endpoints with proper role
7. **Error Handling**: Test invalid requests, insufficient credits, unauthorized access

#### Integration Notes
- Base URL: `http://localhost:8081` (configurable via `server.port`)
- CORS enabled for `http://localhost:5173` (frontend)
- Webhook endpoints must be publicly accessible for payment providers
- Use ngrok or similar for local webhook testing
- JWT tokens expire based on configuration (default: 24 hours)
- Rate limiting not implemented (consider adding for production)

---

## Summary

AI Studio is a production-ready AI content generation platform with:
- **32 database tables** following proper normalization
- **50+ API endpoints** across 13 controllers
- **Multi-provider AI integration** (Gemini, OpenAI, Replicate)
- **Flexible payment system** (Stripe, Razorpay, UPI)
- **Credit-based billing** with transaction history
- **Workspace collaboration** with role-based access
- **Community marketplace** for prompt sharing
- **Comprehensive audit logging** and notifications
- **Admin dashboard** for platform management

The system is designed for scalability with async job processing, proper database relationships, and extensible architecture for adding new AI providers and payment methods.
