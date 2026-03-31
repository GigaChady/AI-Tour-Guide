import uuid
from datetime import datetime
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy import Column, Float, String, Boolean, DateTime, ForeignKey, Index, ARRAY
from geoalchemy2 import Geometry # do zrobienia jakos potem, moze sie przyda do przechowywania trasy czy lokalizacji
from sqlalchemy.orm import relationship
from sqlalchemy import JSON
from sqlalchemy.orm import DeclarativeBase

class Base(DeclarativeBase):
    pass

class User(Base):
    __tablename__ = "users"
    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    email = Column(String, unique=True, nullable=False)
    hashed_password = Column(String, nullable=True) # nullable for Google-auth users
    google_id = Column(String, unique=True, nullable=True)
    is_active = Column(Boolean, default=True)
    created_at = Column(DateTime, default=datetime.utcnow)
    imie = Column(String, nullable=True)
    nazwisko = Column(String, nullable=True)
    plec = Column(String, nullable=True)  
    wiek = Column(Float, nullable=True)
    preferences = relationship("UserPreferences", back_populates="user", uselist=False)
    # routes = relationship("Route", back_populates="user")
    refresh_tokens = relationship("RefreshToken", back_populates="user")


class UserPreferences(Base):
    __tablename__ = "user_preferences"
    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    user_id = Column(UUID(as_uuid=True), ForeignKey("users.id"), unique=True)
    interests = Column(JSON, default=list) # moze jakis enum albo cos zeby ograniczyc do konkretnych kategorii, ale na razie niech bedzie string

    user = relationship("User", back_populates="preferences")


class Route(Base):
    __tablename__ = "routes"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    user_id = Column(UUID(as_uuid=True), ForeignKey("users.id"))
    started_at = Column(DateTime, default=datetime.utcnow)
    ended_at = Column(DateTime, nullable=True)
    city = Column(String, nullable=True)
    name = Column(String, nullable=True) # moze jakis tytul trasy czy cos, zeby latwiej bylo potem rozpoznac trasy usera, ale na razie niech bedzie nullable, bo moze niektorym userom bedzie sie chcialo to wypelniac a innym nie
    # moze jeszcze jakies pola typu aktualna lokalizacja czy cos
    path = Column(Geometry("LINESTRING"), nullable=True) # do zrobienia jakos potem, moze sie przyda do przechowywania trasy
    distance_m = Column(Float, nullable=True)

    user = relationship("User", back_populates="routes")

    __table_args__ = (
        Index("ix_routes_user_id", "user_id"),
    )


class RefreshToken(Base):
    __tablename__ = "refresh_tokens"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    user_id = Column(UUID(as_uuid=True), ForeignKey("users.id"))
    token_hash = Column(String, nullable=False)
    expires_at = Column(DateTime(timezone=True), nullable=False)
    revoked = Column(Boolean, default=False)

    user = relationship("User", back_populates="refresh_tokens")

    __table_args__ = (
        Index("ix_refresh_tokens_user_id_token_hash", "user_id"),
    )

