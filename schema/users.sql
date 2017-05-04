BEGIN TRANSACTION;
CREATE TABLE "UserPortfolios" (
	`PortfolioId`	TEXT NOT NULL UNIQUE,
	`Name`	TEXT,
	`UserId`	TEXT NOT NULL,
	'PoolId' TEXT,
	PRIMARY KEY(`PortfolioId`),
	FOREIGN KEY('PoolId') REFERENCES 'Pools'('PoolId') ON UPDATE CASCADE
);
CREATE TABLE "Pools" (
	'PoolId' TEXT NOT NULL UNIQUE,
	'Name' TEXT NOT NULL,
	'Balance' INTEGER NOT NULL,
	'Start' INTEGER NOT NULL,
	'End' INTEGER
);
CREATE TABLE "History" (
	`portfolio`	TEXT NOT NULL,
	`stock`	TEXT NOT NULL,
	`time`	INTEGER NOT NULL,
	`trans`	INTEGER NOT NULL,
	`price`	REAL NOT NULL,
	FOREIGN KEY(`portfolio`) REFERENCES `UserPortfolios`(`PortfolioId`) ON DELETE CASCADE ON UPDATE CASCADE
);
CREATE TABLE "Balances" (
	`portfolio`	TEXT NOT NULL UNIQUE,
	`balance`	REAL NOT NULL DEFAULT 1000000,
	PRIMARY KEY(`portfolio`),
	FOREIGN KEY(`portfolio`) REFERENCES `UserPortfolios`(`PortfolioId`) ON DELETE CASCADE ON UPDATE CASCADE
);
CREATE TABLE `Users` (
	`UserId`	TEXT,
	`Name`	TEXT,
	`Pic`	TEXT,
	`Email`	TEXT,
	PRIMARY KEY(`UserId`)
);
COMMIT;
