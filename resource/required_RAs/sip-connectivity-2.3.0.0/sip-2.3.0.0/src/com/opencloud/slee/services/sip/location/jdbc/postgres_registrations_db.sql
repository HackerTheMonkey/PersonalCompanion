create table registrations (
  sipaddress varchar(80) not null,
  contactaddress varchar(80) not null,
  expiry bigint,
  qvalue integer,
  cseq integer,
  callid varchar(80),
  flowid varchar(80),
  primary key (sipaddress, contactaddress)
);
COMMENT ON TABLE registrations IS 'SIP Location Service registrations';
