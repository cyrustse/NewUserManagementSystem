# User Management System - User Guide

## Overview

The User Management System (UMS) is a web-based application for managing users, roles, and permissions. This guide covers all features and workflows.

## Table of Contents

1. [Getting Started](#getting-started)
2. [Authentication](#authentication)
3. [User Management](#user-management)
4. [Role Management](#role-management)
5. [Permission Management](#permission-management)
6. [Troubleshooting](#troubleshooting)

---

## Getting Started

### Access the Application

Navigate to `http://localhost:3000` in your browser. The login page will appear.

### System Requirements

- Modern web browser (Chrome, Firefox, Safari, Edge)
- JavaScript enabled
- HTTPS-capable browser (required for cookie-based auth)

---

## Authentication

### Logging In

1. Enter your username or email
2. Enter your password
3. Click "Sign In"

### Session Management

- **Stay logged in**: Tokens are stored securely in httpOnly cookies
- **Session persistence**: You won't be logged out when refreshing the page
- **Session timeout**: Access tokens expire after 15 minutes; refresh tokens last 7 days

### Logging Out

Click your profile icon in the top-right corner and select "Sign Out". All session cookies will be cleared.

---

## User Management

### Viewing Users

Navigate to the **Users** page from the sidebar. The user list displays:
- Username
- Email
- Status (ACTIVE, INACTIVE, PENDING)
- Roles
- Created date

### Creating a New User

1. Click **+ New User** button
2. Fill in the required fields:
   - **Username** (unique)
   - **Email** (unique)
   - **Password**
3. Optionally assign roles
4. Click **Create User**

### Editing a User

1. Find the user in the list
2. Click the **Edit** icon (pencil)
3. Modify fields as needed
4. Click **Save Changes**

### Activating/Deactivating Users

Use the status toggle button in the user list:
- **Active** status → Click to deactivate
- **Inactive/Pending** status → Click to activate

### Managing User Roles

1. Find the user in the list
2. Click **Manage Roles** button
3. View current roles in the dropdown
4. Select or deselect roles as needed
5. Changes apply immediately

### Deleting a User

1. Find the user in the list
2. Click the **Delete** icon (trash)
3. Confirm the deletion in the dialog

---

## Role Management

### Understanding Roles

Roles define what users can do in the system. Each role has:
- **Name**: Unique identifier (e.g., SUPER_ADMIN, EDITOR)
- **Description**: Human-readable purpose
- **Priority**: Higher priority roles take precedence
- **System roles**: Cannot be modified or deleted

### Viewing Roles

Navigate to the **Roles** page. Each role displays:
- Name and description
- Number of users with this role
- Priority level
- Whether it's a system role

### Creating a New Role

1. Click **+ New Role** button
2. Fill in:
   - **Name**: Unique role name
   - **Description**: What this role is for
   - **Priority**: Numerical priority (higher = more important)
3. Click **Create Role**

### Assigning Permissions to Roles

1. Click **Edit** on a role
2. View current permissions
3. Add/remove permissions as needed
4. Click **Save**

### Deleting Roles

- System roles cannot be deleted
- Non-system roles can be deleted if no users are assigned

---

## Permission Management

### Understanding Permissions

Permissions control access to specific actions:
- **Resource**: What object is being accessed (e.g., users, roles, documents)
- **Action**: What can be done (create, read, update, delete)
- **Conditions**: Optional restrictions (e.g., "own data only")

### Viewing Permissions

Navigate to the **Permissions** page to see all permissions in the system.

### Creating a New Permission

1. Click **+ New Permission** button
2. Fill in:
   - **Name**: Unique permission name (e.g., user:read)
   - **Resource**: The resource type this applies to
   - **Action**: create, read, update, or delete
   - **Conditions**: Optional JSON conditions
3. Click **Create Permission**

---

## Role Hierarchy

Roles can have parent-child relationships. Child roles inherit:
- All permissions from their parent
- Hierarchy is determined by priority

### Setting Parent Role

1. Edit a role
2. Select a parent role from the dropdown
3. Save changes

---

## Troubleshooting

### Common Issues

#### "Invalid permission ID format"
- Ensure the ID is a valid UUID format

#### Role won't save
- Check that the name is unique
- Priority must be a number

#### User count shows 0 for a role
- Users must be explicitly assigned to roles
- Check "Manage Roles" to verify assignments

#### Cannot delete a role
- System roles cannot be deleted
- Remove all users from the role first

#### Session logged out unexpectedly
- Access token expired (15 min default)
- Refresh your page to get a new token
- If issue persists, log out and log back in

### Browser Console Errors

| Error | Solution |
|-------|----------|
| 401 Unauthorized | Log in again |
| 403 Forbidden | Contact admin for access |
| 500 Internal Error | Report to system administrator |

---

## Best Practices

1. **Use descriptive names**: Role names like `content-editor` are clearer than `ce`
2. **Follow naming conventions**: Use format `resource:action` (e.g., `user:create`)
3. **Minimize privileges**: Assign only necessary permissions
4. **Regular audits**: Review user roles periodically
5. **Document changes**: Note why role/permission changes were made

---

## Keyboard Shortcuts

| Key | Action |
|-----|--------|
| `Ctrl/Cmd + K` | Quick search |
| `Esc` | Close modal/dialog |
| `Enter` | Confirm action |
| `Tab` | Navigate form fields |

---

## Support

For issues not covered in this guide:
1. Check browser console for error messages
2. Contact system administrator
3. Review server logs for backend errors
