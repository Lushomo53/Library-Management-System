-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Generation Time: Jan 19, 2026 at 10:45 PM
-- Server version: 10.4.32-MariaDB
-- PHP Version: 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `library_db`
--

DELIMITER $$
--
-- Procedures
--
CREATE DEFINER=`root`@`localhost` PROCEDURE `calculate_overdue_fines` ()   BEGIN
    UPDATE borrowed_books
    SET 
        status = 'OVERDUE',
        fine_amount = DATEDIFF(CURRENT_DATE, due_date) * 0.50 
    WHERE due_date < CURRENT_DATE 
    AND status = 'ISSUED';
END$$

CREATE DEFINER=`root`@`localhost` PROCEDURE `check_book_availability` (IN `book_isbn` VARCHAR(20))   BEGIN
    SELECT 
        book_id,
        title,
        author,
        available_copies,
        total_copies,
        CASE 
            WHEN available_copies > 0 THEN 'Available'
            ELSE 'Not Available'
        END as availability_status
    FROM books
    WHERE isbn = book_isbn;
END$$

DELIMITER ;

-- --------------------------------------------------------

--
-- Table structure for table `books`
--

CREATE TABLE `books` (
  `book_id` int(11) NOT NULL,
  `isbn` varchar(20) NOT NULL,
  `title` varchar(255) NOT NULL,
  `author` varchar(100) NOT NULL,
  `publisher` varchar(100) DEFAULT NULL,
  `publication_year` int(11) DEFAULT NULL,
  `edition` varchar(50) DEFAULT NULL,
  `category` varchar(50) NOT NULL,
  `description` text DEFAULT NULL,
  `total_copies` int(11) NOT NULL DEFAULT 1,
  `available_copies` int(11) NOT NULL DEFAULT 1,
  `price` decimal(10,2) DEFAULT NULL,
  `shelf_location` varchar(50) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ;

--
-- Dumping data for table `books`
--

INSERT INTO `books` (`book_id`, `isbn`, `title`, `author`, `publisher`, `publication_year`, `edition`, `category`, `description`, `total_copies`, `available_copies`, `price`, `shelf_location`, `created_at`, `updated_at`) VALUES
(1, '978-0-13-468599-1', 'Clean Code', 'Robert C. Martin', 'Prentice Hall', 2008, '2nd Edition', 'Technology', 'A handbook of agile software craftsmanship', 5, 5, 42.99, 'A-12', '2026-01-01 10:35:39', '2026-01-19 21:35:08'),
(2, '978-0-13-235088-4', 'The Clean Coder', 'Robert C. Martin', 'Prentice Hall', 2011, NULL, 'Technology', 'A code of conduct for professional programmers', 3, 3, 39.99, 'A-13', '2026-01-01 10:35:39', '2026-01-19 17:21:14'),
(3, '978-0-13-957735-1', 'The Pragmatic Programmer', 'Andrew Hunt', 'Addison-Wesley', 1999, NULL, 'Technology', 'From journeyman to master', 4, 4, 44.99, 'A-14', '2026-01-01 10:35:39', '2026-01-19 21:40:22'),
(4, '978-0-7432-7356-5', 'The Great Gatsby', 'F. Scott Fitzgerald', 'Scribner', 1925, NULL, 'Fiction', 'A classic American novel', 10, 10, 15.99, 'B-20', '2026-01-01 10:35:39', '2026-01-19 17:08:19'),
(5, '978-0-06-112008-4', 'To Kill a Mockingbird', 'Harper Lee', 'Harper Perennial', 1960, NULL, 'Fiction', 'A gripping tale of racial injustice', 8, 8, 18.99, 'B-21', '2026-01-01 10:35:39', '2026-01-19 17:08:19'),
(6, '978-0-14-303943-3', '1984', 'George Orwell', 'Penguin Books', 1949, NULL, 'Fiction', 'A dystopian social science fiction', 6, 6, 16.99, 'B-22', '2026-01-01 10:35:39', '2026-01-18 17:05:13');

-- --------------------------------------------------------

--
-- Table structure for table `borrowed_books`
--

CREATE TABLE `borrowed_books` (
  `borrow_id` int(11) NOT NULL,
  `request_id` int(11) NOT NULL,
  `member_id` int(11) NOT NULL,
  `book_id` int(11) NOT NULL,
  `issued_by` int(11) NOT NULL,
  `issue_date` timestamp NOT NULL DEFAULT current_timestamp(),
  `due_date` date NOT NULL,
  `return_date` timestamp NULL DEFAULT NULL,
  `returned_to` int(11) DEFAULT NULL,
  `status` enum('ISSUED','RETURNED','OVERDUE') DEFAULT 'ISSUED',
  `allow_renewal` tinyint(1) DEFAULT 1,
  `renewal_count` int(11) DEFAULT 0,
  `fine_amount` decimal(10,2) DEFAULT 0.00,
  `notes` text DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `borrowed_books`
--

INSERT INTO `borrowed_books` (`borrow_id`, `request_id`, `member_id`, `book_id`, `issued_by`, `issue_date`, `due_date`, `return_date`, `returned_to`, `status`, `allow_renewal`, `renewal_count`, `fine_amount`, `notes`) VALUES
(1, 2, 13, 3, 2, '2026-01-19 21:39:17', '2026-02-02', '2026-01-19 21:40:22', 2, 'RETURNED', 1, 0, 0.00, '');

--
-- Triggers `borrowed_books`
--
DELIMITER $$
CREATE TRIGGER `after_book_issued` AFTER INSERT ON `borrowed_books` FOR EACH ROW BEGIN
    IF NEW.status = 'ISSUED' THEN
        UPDATE books 
        SET available_copies = available_copies - 1 
        WHERE book_id = NEW.book_id;
    END IF;
END
$$
DELIMITER ;
DELIMITER $$
CREATE TRIGGER `after_book_returned` AFTER UPDATE ON `borrowed_books` FOR EACH ROW BEGIN
    IF OLD.status = 'ISSUED' AND NEW.status = 'RETURNED' THEN
        UPDATE books 
        SET available_copies = available_copies + 1 
        WHERE book_id = NEW.book_id;
    END IF;
END
$$
DELIMITER ;

-- --------------------------------------------------------

--
-- Table structure for table `borrow_requests`
--

CREATE TABLE `borrow_requests` (
  `request_id` int(11) NOT NULL,
  `member_id` int(11) NOT NULL,
  `book_id` int(11) NOT NULL,
  `request_date` timestamp NOT NULL DEFAULT current_timestamp(),
  `status` enum('PENDING','APPROVED','REJECTED','CANCELLED') DEFAULT 'PENDING',
  `approved_by` int(11) DEFAULT NULL,
  `approved_date` timestamp NULL DEFAULT NULL,
  `borrow_duration_days` int(11) DEFAULT NULL,
  `notes` text DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `borrow_requests`
--

INSERT INTO `borrow_requests` (`request_id`, `member_id`, `book_id`, `request_date`, `status`, `approved_by`, `approved_date`, `borrow_duration_days`, `notes`) VALUES
(1, 13, 2, '2026-01-19 21:32:02', 'REJECTED', NULL, NULL, NULL, 'One at a time'),
(2, 13, 3, '2026-01-19 21:32:06', 'APPROVED', 2, '2026-01-19 21:39:17', 14, '');

-- --------------------------------------------------------

--
-- Stand-in structure for view `low_stock_books`
-- (See below for the actual view)
--
CREATE TABLE `low_stock_books` (
`book_id` int(11)
,`isbn` varchar(20)
,`title` varchar(255)
,`author` varchar(100)
,`category` varchar(50)
,`total_copies` int(11)
,`available_copies` int(11)
,`borrowed_copies` bigint(12)
);

-- --------------------------------------------------------

--
-- Stand-in structure for view `member_statistics`
-- (See below for the actual view)
--
CREATE TABLE `member_statistics` (
`user_id` int(11)
,`member_id` varchar(20)
,`full_name` varchar(100)
,`email` varchar(100)
,`status` enum('ACTIVE','INACTIVE','PENDING')
,`total_books_borrowed` bigint(21)
,`currently_borrowed` bigint(21)
,`overdue_books` bigint(21)
);

-- --------------------------------------------------------

--
-- Stand-in structure for view `overdue_books_view`
-- (See below for the actual view)
--
CREATE TABLE `overdue_books_view` (
`borrow_id` int(11)
,`member_name` varchar(100)
,`email` varchar(100)
,`phone` varchar(20)
,`book_title` varchar(255)
,`author` varchar(100)
,`issue_date` timestamp
,`due_date` date
,`days_overdue` int(7)
,`fine_amount` decimal(10,2)
);

-- --------------------------------------------------------

--
-- Table structure for table `role_id_sequence`
--

CREATE TABLE `role_id_sequence` (
  `role` varchar(20) NOT NULL,
  `next_value` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `role_id_sequence`
--

INSERT INTO `role_id_sequence` (`role`, `next_value`) VALUES
('ADMIN', 2),
('LIBRARIAN', 4),
('MEMBER', 3);

-- --------------------------------------------------------

--
-- Table structure for table `users`
--

CREATE TABLE `users` (
  `user_id` int(11) NOT NULL,
  `username` varchar(50) NOT NULL,
  `password` varchar(255) NOT NULL,
  `role` enum('MEMBER','LIBRARIAN','ADMIN') NOT NULL,
  `full_name` varchar(100) NOT NULL,
  `email` varchar(100) NOT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `address` text DEFAULT NULL,
  `status` enum('ACTIVE','INACTIVE','PENDING') DEFAULT 'PENDING',
  `employee_id` varchar(20) DEFAULT NULL,
  `member_id` varchar(20) DEFAULT NULL,
  `can_approve_requests` tinyint(1) DEFAULT 0,
  `can_issue_returns` tinyint(1) DEFAULT 0,
  `can_revoke_membership` tinyint(1) DEFAULT 0,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `users`
--

INSERT INTO `users` (`user_id`, `username`, `password`, `role`, `full_name`, `email`, `phone`, `address`, `status`, `employee_id`, `member_id`, `can_approve_requests`, `can_issue_returns`, `can_revoke_membership`, `created_at`, `updated_at`) VALUES
(1, 'admin', '$2a$12$VprsaKMBuC2z26UvDM3yCe4AAEtT1luk04T.JmZ35qzpisx.qXTIy', 'ADMIN', 'System Administrator', 'admin@library.com', '+1-555-0001', NULL, 'ACTIVE', 'ADM-0001', NULL, 1, 1, 1, '2026-01-01 10:35:39', '2026-01-19 21:24:19'),
(2, 'librarian1', '$2a$12$Q2fNQNpLGgw50uUJRF5FhuW3gA4kcQ5uwDhH4L.Upv4b2GHZE9nPK', 'LIBRARIAN', 'Jane Smith', 'blantire01@gmail.com', '+1-555-0002', NULL, 'ACTIVE', 'LIB-0002', NULL, 1, 1, 0, '2026-01-01 10:35:39', '2026-01-19 17:07:00'),
(13, 'lushhh21', '$2a$12$8pAUYmbeGTH5kAZR6yj4K.VMCgGK33weuFQanS33rsF4SJJOAf346', 'MEMBER', 'Lushomo Lungo', 'lungolushomo21@gmail.com', '0978117416', 'Lusaka, Zambia', 'INACTIVE', NULL, 'MEM-0001', 0, 0, 0, '2026-01-11 12:27:21', '2026-01-19 21:41:07'),
(18, 'flockster', '$2a$12$qWoFfMfEEOtQ/VabtHhPVepX4H05644p8PMrF8ksIDQSf.m/JVOdO', 'MEMBER', 'Rodney Chilambwe', 'llushomo3@gmail.com', '0978117416', 'Kitwe, Zambia', 'ACTIVE', NULL, 'MEM-0002', 0, 0, 0, '2026-01-19 21:33:08', '2026-01-19 21:33:55'),
(20, 'mkwizera', '$2a$12$X8/rlxn9TlqGlQYg4fu.w.7W/udoSeu6sNxHZ2yV.JjkQUgP2kDw2', 'LIBRARIAN', 'Migisha Kwizera', 'blantiresavgelevel21@gmail.com', '0978117416', 'Chirundu, Zambia', 'INACTIVE', 'LIB-0003', NULL, 1, 1, 1, '2026-01-19 21:37:59', '2026-01-19 21:41:16');

--
-- Triggers `users`
--
DELIMITER $$
CREATE TRIGGER `before_member_insert` BEFORE INSERT ON `users` FOR EACH ROW BEGIN
    IF NEW.role = 'MEMBER' AND NEW.member_id IS NULL THEN
        SET NEW.member_id = CONCAT('MEM-', LPAD(NEW.user_id, 6, '0'));
    END IF;
    IF NEW.role = 'LIBRARIAN' AND NEW.employee_id IS NULL THEN
        SET NEW.employee_id = CONCAT('LIB-', LPAD(NEW.user_id, 6, '0'));
    END IF;
END
$$
DELIMITER ;

-- --------------------------------------------------------

--
-- Structure for view `low_stock_books`
--
DROP TABLE IF EXISTS `low_stock_books`;

CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `low_stock_books`  AS SELECT `books`.`book_id` AS `book_id`, `books`.`isbn` AS `isbn`, `books`.`title` AS `title`, `books`.`author` AS `author`, `books`.`category` AS `category`, `books`.`total_copies` AS `total_copies`, `books`.`available_copies` AS `available_copies`, `books`.`total_copies`- `books`.`available_copies` AS `borrowed_copies` FROM `books` WHERE `books`.`available_copies` < 3 OR `books`.`available_copies` * 100.0 / `books`.`total_copies` < 30 ;

-- --------------------------------------------------------

--
-- Structure for view `member_statistics`
--
DROP TABLE IF EXISTS `member_statistics`;

CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `member_statistics`  AS SELECT `u`.`user_id` AS `user_id`, `u`.`member_id` AS `member_id`, `u`.`full_name` AS `full_name`, `u`.`email` AS `email`, `u`.`status` AS `status`, count(distinct `bb`.`borrow_id`) AS `total_books_borrowed`, count(distinct case when `bb`.`status` = 'ISSUED' then `bb`.`borrow_id` end) AS `currently_borrowed`, count(distinct case when `bb`.`status` = 'OVERDUE' then `bb`.`borrow_id` end) AS `overdue_books` FROM (`users` `u` left join `borrowed_books` `bb` on(`u`.`user_id` = `bb`.`member_id`)) WHERE `u`.`role` = 'MEMBER' GROUP BY `u`.`user_id` ;

-- --------------------------------------------------------

--
-- Structure for view `overdue_books_view`
--
DROP TABLE IF EXISTS `overdue_books_view`;

CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `overdue_books_view`  AS SELECT `bb`.`borrow_id` AS `borrow_id`, `u`.`full_name` AS `member_name`, `u`.`email` AS `email`, `u`.`phone` AS `phone`, `b`.`title` AS `book_title`, `b`.`author` AS `author`, `bb`.`issue_date` AS `issue_date`, `bb`.`due_date` AS `due_date`, to_days(curdate()) - to_days(`bb`.`due_date`) AS `days_overdue`, `bb`.`fine_amount` AS `fine_amount` FROM ((`borrowed_books` `bb` join `users` `u` on(`bb`.`member_id` = `u`.`user_id`)) join `books` `b` on(`bb`.`book_id` = `b`.`book_id`)) WHERE `bb`.`due_date` < curdate() AND `bb`.`status` = 'ISSUED' ;

--
-- Indexes for dumped tables
--

--
-- Indexes for table `books`
--
ALTER TABLE `books`
  ADD PRIMARY KEY (`book_id`),
  ADD UNIQUE KEY `isbn` (`isbn`),
  ADD KEY `idx_isbn` (`isbn`),
  ADD KEY `idx_title` (`title`),
  ADD KEY `idx_author` (`author`),
  ADD KEY `idx_category` (`category`);

--
-- Indexes for table `borrowed_books`
--
ALTER TABLE `borrowed_books`
  ADD PRIMARY KEY (`borrow_id`),
  ADD KEY `request_id` (`request_id`),
  ADD KEY `issued_by` (`issued_by`),
  ADD KEY `returned_to` (`returned_to`),
  ADD KEY `idx_member` (`member_id`),
  ADD KEY `idx_book` (`book_id`),
  ADD KEY `idx_status` (`status`),
  ADD KEY `idx_due_date` (`due_date`),
  ADD KEY `idx_issue_date` (`issue_date`);

--
-- Indexes for table `borrow_requests`
--
ALTER TABLE `borrow_requests`
  ADD PRIMARY KEY (`request_id`),
  ADD KEY `approved_by` (`approved_by`),
  ADD KEY `idx_member` (`member_id`),
  ADD KEY `idx_book` (`book_id`),
  ADD KEY `idx_status` (`status`),
  ADD KEY `idx_request_date` (`request_date`);

--
-- Indexes for table `role_id_sequence`
--
ALTER TABLE `role_id_sequence`
  ADD PRIMARY KEY (`role`);

--
-- Indexes for table `users`
--
ALTER TABLE `users`
  ADD PRIMARY KEY (`user_id`),
  ADD UNIQUE KEY `username` (`username`),
  ADD UNIQUE KEY `email` (`email`),
  ADD UNIQUE KEY `employee_id` (`employee_id`),
  ADD UNIQUE KEY `member_id` (`member_id`),
  ADD UNIQUE KEY `uq_employee_id` (`employee_id`),
  ADD UNIQUE KEY `uq_member_id` (`member_id`),
  ADD KEY `idx_username` (`username`),
  ADD KEY `idx_email` (`email`),
  ADD KEY `idx_role` (`role`),
  ADD KEY `idx_status` (`status`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `books`
--
ALTER TABLE `books`
  MODIFY `book_id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `borrowed_books`
--
ALTER TABLE `borrowed_books`
  MODIFY `borrow_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT for table `borrow_requests`
--
ALTER TABLE `borrow_requests`
  MODIFY `request_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT for table `users`
--
ALTER TABLE `users`
  MODIFY `user_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=21;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `borrowed_books`
--
ALTER TABLE `borrowed_books`
  ADD CONSTRAINT `borrowed_books_ibfk_1` FOREIGN KEY (`request_id`) REFERENCES `borrow_requests` (`request_id`) ON DELETE CASCADE,
  ADD CONSTRAINT `borrowed_books_ibfk_2` FOREIGN KEY (`member_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE,
  ADD CONSTRAINT `borrowed_books_ibfk_3` FOREIGN KEY (`book_id`) REFERENCES `books` (`book_id`) ON DELETE CASCADE,
  ADD CONSTRAINT `borrowed_books_ibfk_4` FOREIGN KEY (`issued_by`) REFERENCES `users` (`user_id`) ON DELETE CASCADE,
  ADD CONSTRAINT `borrowed_books_ibfk_5` FOREIGN KEY (`returned_to`) REFERENCES `users` (`user_id`) ON DELETE SET NULL;

--
-- Constraints for table `borrow_requests`
--
ALTER TABLE `borrow_requests`
  ADD CONSTRAINT `borrow_requests_ibfk_1` FOREIGN KEY (`member_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE,
  ADD CONSTRAINT `borrow_requests_ibfk_2` FOREIGN KEY (`book_id`) REFERENCES `books` (`book_id`) ON DELETE CASCADE,
  ADD CONSTRAINT `borrow_requests_ibfk_3` FOREIGN KEY (`approved_by`) REFERENCES `users` (`user_id`) ON DELETE SET NULL;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
