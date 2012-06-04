create table registrations (
  sipaddress varchar(80) not null,
  contactaddress varchar(80) not null,
  expiry number(22),
  qvalue number(5),
  cseq number(9),
  callid varchar(80),
  flowid varchar(80)
);
CREATE UNIQUE INDEX PK_registrations ON registrations (sipaddress, contactaddress);
COMMENT ON TABLE registrations IS 'SIP Location Service registrations';
