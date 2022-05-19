--
-- PostgreSQL database cluster dump
--

SET default_transaction_read_only = off;

SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;

--
-- Roles
--

CREATE ROLE postgres;
ALTER ROLE postgres WITH SUPERUSER INHERIT CREATEROLE CREATEDB LOGIN REPLICATION BYPASSRLS PASSWORD 'md53175bce1d3201d16594cebf9d7eb3f9d';






--
-- Databases
--

--
-- Database "postgres" dump
--

\connect postgres

--
-- PostgreSQL database dump
--

-- Dumped from database version 12.8
-- Dumped by pg_dump version 12.10 (Ubuntu 12.10-0ubuntu0.20.04.1)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: kiwi_mod_data_export_spring; Type: SCHEMA; Schema: -; Owner: postgres
--

CREATE SCHEMA kiwi_mod_data_export_spring;


ALTER SCHEMA kiwi_mod_data_export_spring OWNER TO postgres;

--
-- Name: pgcrypto; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public;


--
-- Name: EXTENSION pgcrypto; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION pgcrypto IS 'cryptographic functions';


--
-- Name: batchstatus; Type: TYPE; Schema: kiwi_mod_data_export_spring; Owner: postgres
--

CREATE TYPE kiwi_mod_data_export_spring.batchstatus AS ENUM (
    'COMPLETED',
    'STARTING',
    'STARTED',
    'STOPPING',
    'STOPPED',
    'FAILED',
    'ABANDONED',
    'UNKNOWN'
);


ALTER TYPE kiwi_mod_data_export_spring.batchstatus OWNER TO postgres;

--
-- Name: exporttype; Type: TYPE; Schema: kiwi_mod_data_export_spring; Owner: postgres
--

CREATE TYPE kiwi_mod_data_export_spring.exporttype AS ENUM (
    'CIRCULATION_LOG',
    'BURSAR_FEES_FINES'
);


ALTER TYPE kiwi_mod_data_export_spring.exporttype OWNER TO postgres;

--
-- Name: jobstatus; Type: TYPE; Schema: kiwi_mod_data_export_spring; Owner: postgres
--

CREATE TYPE kiwi_mod_data_export_spring.jobstatus AS ENUM (
    'SCHEDULED',
    'IN_PROGRESS',
    'SUCCESSFUL',
    'FAILED'
);


ALTER TYPE kiwi_mod_data_export_spring.jobstatus OWNER TO postgres;

--
-- Name: CAST (character varying AS kiwi_mod_data_export_spring.batchstatus); Type: CAST; Schema: -; Owner: -
--

CREATE CAST (character varying AS kiwi_mod_data_export_spring.batchstatus) WITH INOUT AS IMPLICIT;


--
-- Name: CAST (character varying AS kiwi_mod_data_export_spring.exporttype); Type: CAST; Schema: -; Owner: -
--

CREATE CAST (character varying AS kiwi_mod_data_export_spring.exporttype) WITH INOUT AS IMPLICIT;


--
-- Name: CAST (character varying AS kiwi_mod_data_export_spring.jobstatus); Type: CAST; Schema: -; Owner: -
--

CREATE CAST (character varying AS kiwi_mod_data_export_spring.jobstatus) WITH INOUT AS IMPLICIT;


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: databasechangelog; Type: TABLE; Schema: kiwi_mod_data_export_spring; Owner: postgres
--

CREATE TABLE kiwi_mod_data_export_spring.databasechangelog (
    id character varying(255) NOT NULL,
    author character varying(255) NOT NULL,
    filename character varying(255) NOT NULL,
    dateexecuted timestamp without time zone NOT NULL,
    orderexecuted integer NOT NULL,
    exectype character varying(10) NOT NULL,
    md5sum character varying(35),
    description character varying(255),
    comments character varying(255),
    tag character varying(255),
    liquibase character varying(20),
    contexts character varying(255),
    labels character varying(255),
    deployment_id character varying(10)
);


ALTER TABLE kiwi_mod_data_export_spring.databasechangelog OWNER TO postgres;

--
-- Name: databasechangeloglock; Type: TABLE; Schema: kiwi_mod_data_export_spring; Owner: postgres
--

CREATE TABLE kiwi_mod_data_export_spring.databasechangeloglock (
    id integer NOT NULL,
    locked boolean NOT NULL,
    lockgranted timestamp without time zone,
    lockedby character varying(255)
);


ALTER TABLE kiwi_mod_data_export_spring.databasechangeloglock OWNER TO postgres;

--
-- Name: job; Type: TABLE; Schema: kiwi_mod_data_export_spring; Owner: postgres
--

CREATE TABLE kiwi_mod_data_export_spring.job (
    id uuid DEFAULT public.gen_random_uuid() NOT NULL,
    name character varying(100),
    description text,
    source character varying(50),
    is_system_source boolean,
    type kiwi_mod_data_export_spring.exporttype NOT NULL,
    export_type_specific_parameters jsonb NOT NULL,
    status kiwi_mod_data_export_spring.jobstatus DEFAULT 'SCHEDULED'::kiwi_mod_data_export_spring.jobstatus,
    files jsonb,
    start_time timestamp without time zone,
    end_time timestamp without time zone,
    created_date timestamp without time zone DEFAULT now() NOT NULL,
    created_by_user_id uuid,
    created_by_username character varying(50),
    updated_date timestamp without time zone,
    updated_by_user_id uuid,
    updated_by_username character varying(50),
    output_format character varying(50),
    error_details text,
    batch_status kiwi_mod_data_export_spring.batchstatus,
    exit_status jsonb
);


ALTER TABLE kiwi_mod_data_export_spring.job OWNER TO postgres;

--
-- Name: job-number; Type: SEQUENCE; Schema: kiwi_mod_data_export_spring; Owner: postgres
--

CREATE SEQUENCE kiwi_mod_data_export_spring."job-number"
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
    CYCLE;


ALTER TABLE kiwi_mod_data_export_spring."job-number" OWNER TO postgres;

--
-- Data for Name: databasechangelog; Type: TABLE DATA; Schema: kiwi_mod_data_export_spring; Owner: postgres
--

COPY kiwi_mod_data_export_spring.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) FROM stdin;
MODEXPS-1@@create-pgcrypto-extension	prokhorovalexey	db/changelog/changes/create-db.xml	2000-01-01 00:00:00.000	1	EXECUTED	8:a0af0290a06ceaeb2b493ed416b8e7bd	sql		\N	4.3.5	\N	\N	2734610999
MODEXPS-1@@create-export-type-enum	prokhorovalexey	db/changelog/changes/create-db.xml	2000-01-01 00:00:00.000	2	EXECUTED	8:b0ae38617fc57c6168105b9d30bc0b75	sql		\N	4.3.5	\N	\N	2734610999
MODEXPS-1@@create-export-type-enum-cast	prokhorovalexey	db/changelog/changes/create-db.xml	2000-01-01 00:00:00.000	3	EXECUTED	8:2471b72c4978a35e88888ded3d9ea035	sql		\N	4.3.5	\N	\N	2734610999
MODEXPS-1@@create-job-status-enum	prokhorovalexey	db/changelog/changes/create-db.xml	2000-01-01 00:00:00.000	4	EXECUTED	8:78cbb50bf420af2859464b0a5b4294d3	sql		\N	4.3.5	\N	\N	2734610999
MODEXPS-1@@create-job-status-enum-cast	prokhorovalexey	db/changelog/changes/create-db.xml	2000-01-01 00:00:00.000	5	EXECUTED	8:b8f454082eea59e65b9efa9de177cd90	sql		\N	4.3.5	\N	\N	2734610999
MODEXPS-1@@create-batch-status-enum	prokhorovalexey	db/changelog/changes/create-db.xml	2000-01-01 00:00:00.000	6	EXECUTED	8:794c5e48911a4ec56a1b945b4b7c89e5	sql		\N	4.3.5	\N	\N	2734610999
MODEXPS-1@@create-batch-status-enum-cast	prokhorovalexey	db/changelog/changes/create-db.xml	2000-01-01 00:00:00.000	7	EXECUTED	8:84ad8d7c1ec7bb9748c80f18cb38ded1	sql		\N	4.3.5	\N	\N	2734610999
MODEXPS-1@@create-job-table	prokhorovalexey	db/changelog/changes/create-db.xml	2000-01-01 00:00:00.000	8	EXECUTED	8:f05e8f0b3f4606b2fefca41c409a1955	createTable tableName=job		\N	4.3.5	\N	\N	2734610999
MODEXPS-1@@create-job-number-sequence	prokhorovalexey	db/changelog/changes/create-db.xml	2000-01-01 00:00:00.000	9	EXECUTED	8:0402713e9837cdb9f30928ccca931a84	createSequence sequenceName=job-number		\N	4.3.5	\N	\N	2734610999
MODEXPS-23@@change-json-parameter	romanleshchenko	db/changelog/changes/change-input-job-parameters.xml	2000-01-01 00:00:00.000	10	EXECUTED	8:735313531cd154c4cb375f4456817708	sql		\N	4.3.5	\N	\N	2734610999
\.


--
-- Data for Name: databasechangeloglock; Type: TABLE DATA; Schema: kiwi_mod_data_export_spring; Owner: postgres
--

COPY kiwi_mod_data_export_spring.databasechangeloglock (id, locked, lockgranted, lockedby) FROM stdin;
1	f	\N	\N
\.


--
-- Data for Name: job; Type: TABLE DATA; Schema: kiwi_mod_data_export_spring; Owner: postgres
--

COPY kiwi_mod_data_export_spring.job (id, name, description, source, is_system_source, type, export_type_specific_parameters, status, files, start_time, end_time, created_date, created_by_user_id, created_by_username, updated_date, updated_by_user_id, updated_by_username, output_format, error_details, batch_status, exit_status) FROM stdin;
\.


--
-- Name: job-number; Type: SEQUENCE SET; Schema: kiwi_mod_data_export_spring; Owner: postgres
--

SELECT pg_catalog.setval('kiwi_mod_data_export_spring."job-number"', 1, false);


--
-- Name: databasechangeloglock databasechangeloglock_pkey; Type: CONSTRAINT; Schema: kiwi_mod_data_export_spring; Owner: postgres
--

ALTER TABLE ONLY kiwi_mod_data_export_spring.databasechangeloglock
    ADD CONSTRAINT databasechangeloglock_pkey PRIMARY KEY (id);


--
-- Name: job pk_job; Type: CONSTRAINT; Schema: kiwi_mod_data_export_spring; Owner: postgres
--

ALTER TABLE ONLY kiwi_mod_data_export_spring.job
    ADD CONSTRAINT pk_job PRIMARY KEY (id);


--
-- PostgreSQL database dump complete
--

--
-- PostgreSQL database cluster dump complete
--

