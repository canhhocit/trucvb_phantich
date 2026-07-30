ALTER TABLE "organizations" ADD COLUMN "reject_reason" text;
ALTER TABLE "organizations" ALTER COLUMN "status" SET DEFAULT 'PENDING_APPROVAL';
