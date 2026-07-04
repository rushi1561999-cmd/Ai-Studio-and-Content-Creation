-- Rename the 'read' column to 'is_read' in the notifications table
ALTER TABLE notifications CHANGE COLUMN read is_read BIT NOT NULL DEFAULT 0;
